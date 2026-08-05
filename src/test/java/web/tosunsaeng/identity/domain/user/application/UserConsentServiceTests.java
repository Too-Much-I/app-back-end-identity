package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.UserConsentUpdateRequest;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

class UserConsentServiceTests {

	private static final String PRIVACY_VERSION = "privacy-v2";
	private static final String TERM_VERSION = "term-v2";
	private static final Instant NOW = Instant.parse("2026-08-05T09:00:00Z");
	private static final Instant PREVIOUS_AT = Instant.parse("2026-08-01T09:00:00Z");

	private CurrentUserProvider currentUserProvider;
	private UserRepository userRepository;
	private ConsentPolicy consentPolicy;
	private UserConsentService userConsentService;

	@BeforeEach
	void setUp() {
		currentUserProvider = mock(CurrentUserProvider.class);
		userRepository = mock(UserRepository.class);
		consentPolicy = new ConsentPolicy(PRIVACY_VERSION, TERM_VERSION);
		userConsentService = new UserConsentService(
				currentUserProvider,
				userRepository,
				consentPolicy,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void updatesLegacyUserUsingOnlyJwtSubjectAndPersistsBothConsentsTogether() {
		User user = localUser(UserConsents.unconsented());
		ReflectionTestUtils.setField(user, "consents", null);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		UserConsentResponse response = userConsentService.updateConsents(validRequest());

		verify(currentUserProvider).getCurrentUserId();
		verify(userRepository).findById(user.getUserId());
		verify(userRepository).save(user);
		assertThat(response.privacyConsented()).isTrue();
		assertThat(response.privacyConsentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacyConsentedAt()).isEqualTo(NOW);
		assertThat(response.termConsented()).isTrue();
		assertThat(response.termConsentVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.termConsentedAt()).isEqualTo(NOW);
		assertThat(user.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void sameVersionsAreIdempotentAndPreserveTimesWithoutSavingAgain() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		));
		Instant previousUpdatedAt = user.getUpdatedAt();
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentResponse response = userConsentService.updateConsents(validRequest());

		verify(userRepository, never()).save(any(User.class));
		assertThat(response.privacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.termConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(user.getUpdatedAt()).isEqualTo(previousUpdatedAt);
	}

	@Test
	void changedVersionsUseServerTimeForBothConsents() {
		User user = localUser(UserConsents.consented("privacy-v1", "term-v1", PREVIOUS_AT));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		UserConsentResponse response = userConsentService.updateConsents(validRequest());

		assertThat(response.privacyConsentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacyConsentedAt()).isEqualTo(NOW);
		assertThat(response.termConsentVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.termConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void unchangedPrivacyVersionKeepsItsTimeWhileChangedTermUsesServerTime() {
		User user = localUser(UserConsents.consented(PRIVACY_VERSION, "term-v1", PREVIOUS_AT));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(userRepository.save(user)).thenReturn(user);

		UserConsentResponse response = userConsentService.updateConsents(validRequest());

		assertThat(response.privacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.termConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void rejectsFalsePrivacyConsentBeforeLookingUpUser() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						false,
						PRIVACY_VERSION,
						true,
						TERM_VERSION
				))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.PRIVACY_CONSENT_REQUIRED);
		verify(currentUserProvider, never()).getCurrentUserId();
		verify(userRepository, never()).save(any());
	}

	@Test
	void rejectsFalseTermConsentBeforeLookingUpUser() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						true,
						PRIVACY_VERSION,
						false,
						TERM_VERSION
				))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.TERM_CONSENT_REQUIRED);
		verify(currentUserProvider, never()).getCurrentUserId();
	}

	@Test
	void rejectsVersionsThatDoNotMatchServerPolicy() {
		BusinessException privacyFailure = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						true,
						"privacy-v1",
						true,
						TERM_VERSION
				))
		);
		BusinessException termFailure = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						true,
						PRIVACY_VERSION,
						true,
						"term-v1"
				))
		);

		assertThat(privacyFailure.getErrorCode())
				.isEqualTo(UserErrorStatus.PRIVACY_CONSENT_VERSION_MISMATCH);
		assertThat(termFailure.getErrorCode())
				.isEqualTo(UserErrorStatus.TERM_CONSENT_VERSION_MISMATCH);
		verify(userRepository, never()).save(any());
	}

	@Test
	void updateRequestCannotSelectUserOrInstallation() {
		assertThat(Arrays.stream(UserConsentUpdateRequest.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly(
						"isPrivacyConsented",
						"privacyConsentVersion",
						"isTermConsented",
						"termConsentVersion"
				)
				.doesNotContain("userId", "installationId");
	}

	private User localUser(UserConsents consents) {
		return User.create(
				"user@example.com",
				"user@example.com",
				"test-only-password-hash",
				"테스트사용자",
				consents,
				PREVIOUS_AT
		);
	}

	private UserConsentUpdateRequest validRequest() {
		return new UserConsentUpdateRequest(
				true,
				PRIVACY_VERSION,
				true,
				TERM_VERSION
		);
	}
}
