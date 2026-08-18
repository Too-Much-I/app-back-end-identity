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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.UserConsentUpdateRequest;
import web.tosunsaeng.identity.domain.user.dto.response.ConsentPolicyStatusResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentStatusResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.support.LogCapture;

class UserConsentServiceTests {

	private static final String PRIVACY_VERSION = "privacy-v2";
	private static final String TERM_VERSION = "term-v2";
	private static final String QUALITY_REVIEW_VERSION = "quality-review-v2";
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
		consentPolicy = new ConsentPolicy(
				PRIVACY_VERSION,
				TERM_VERSION,
				QUALITY_REVIEW_VERSION
		);
		when(userRepository.updateConsentsIfActive(any(User.class), any(Instant.class)))
				.thenReturn(true);
		userConsentService = new UserConsentService(
				currentUserProvider,
				userRepository,
				consentPolicy,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void getsCurrentConsentStatusUsingOnlyJwtSubjectWithMatchingVersions() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		verify(currentUserProvider).getCurrentUserId();
		verify(userRepository).findById(user.getUserId());
		verify(userRepository, never()).save(any(User.class));
		assertThat(response.privacy().currentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacy().consented()).isTrue();
		assertThat(response.privacy().consentedVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacy().consentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.privacy().requiresConsent()).isFalse();
		assertThat(response.terms().currentVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.terms().consented()).isTrue();
		assertThat(response.terms().consentedVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.terms().consentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.terms().requiresConsent()).isFalse();
		assertThat(response.qualityReview().currentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
		assertThat(response.qualityReview().consented()).isFalse();
		assertThat(response.qualityReview().consentedVersion()).isNull();
		assertThat(response.qualityReview().consentedAt()).isNull();
		assertThat(response.qualityReview().requiresConsent()).isFalse();
	}

	@Test
	void differentPrivacyVersionRequiresOnlyPrivacyConsent() {
		User user = localUser(UserConsents.consented(
				"privacy-v1",
				TERM_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().requiresConsent()).isTrue();
		assertThat(response.terms().requiresConsent()).isFalse();
	}

	@Test
	void differentTermVersionRequiresOnlyTermConsent() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				"term-v1",
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().requiresConsent()).isFalse();
		assertThat(response.terms().requiresConsent()).isTrue();
	}

	@Test
	void falseConsentRequiresBothConsents() {
		User user = localUser(UserConsents.unconsented());
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().consented()).isFalse();
		assertThat(response.privacy().requiresConsent()).isTrue();
		assertThat(response.terms().consented()).isFalse();
		assertThat(response.terms().requiresConsent()).isTrue();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", " "})
	void nullOrBlankStoredVersionRequiresConsent(String storedVersion) {
		UserConsents consents = UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		);
		ReflectionTestUtils.setField(consents, "privacyConsentVersion", storedVersion);
		User user = localUser(consents);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().consented()).isTrue();
		assertThat(response.privacy().consentedVersion()).isEqualTo(storedVersion);
		assertThat(response.privacy().requiresConsent()).isTrue();
		assertThat(response.terms().requiresConsent()).isFalse();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", " "})
	void nullOrBlankStoredTermVersionRequiresConsent(String storedVersion) {
		UserConsents consents = UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		);
		ReflectionTestUtils.setField(consents, "termConsentVersion", storedVersion);
		User user = localUser(consents);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().requiresConsent()).isFalse();
		assertThat(response.terms().consented()).isTrue();
		assertThat(response.terms().consentedVersion()).isEqualTo(storedVersion);
		assertThat(response.terms().requiresConsent()).isTrue();
	}

	@Test
	void versionComparisonUsesExactStringEquality() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION.toUpperCase(java.util.Locale.ROOT),
				TERM_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().requiresConsent()).isTrue();
	}

	@Test
	void legacyUserWithoutConsentFieldsIsReturnedAsUnconsented() {
		User user = localUser(UserConsents.unconsented());
		ReflectionTestUtils.setField(user, "consents", null);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentStatusResponse response = userConsentService.getCurrentConsentStatus();

		assertThat(response.privacy().consented()).isFalse();
		assertThat(response.privacy().consentedVersion()).isNull();
		assertThat(response.privacy().consentedAt()).isNull();
		assertThat(response.privacy().requiresConsent()).isTrue();
		assertThat(response.terms().consented()).isFalse();
		assertThat(response.terms().consentedVersion()).isNull();
		assertThat(response.terms().consentedAt()).isNull();
		assertThat(response.terms().requiresConsent()).isTrue();
	}

	@Test
	void consentStatusReturnsExistingUserNotFoundError() {
		when(currentUserProvider.getCurrentUserId()).thenReturn("missing-user-id");
		when(userRepository.findById("missing-user-id")).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				userConsentService::getCurrentConsentStatus
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.USER_NOT_FOUND);
	}

	@Test
	void consentStatusResponseDoesNotExposeUserOrInstallationIdentifiers() {
		assertThat(Arrays.stream(UserConsentStatusResponse.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly("privacy", "terms", "qualityReview")
				.doesNotContain("userId", "installationId", "guestInstallationIdHash");
		assertThat(Arrays.stream(ConsentPolicyStatusResponse.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly(
						"currentVersion",
						"consented",
						"consentedVersion",
						"consentedAt",
						"requiresConsent"
				);
	}

	@Test
	void updatesLegacyUserUsingOnlyJwtSubjectAndPersistsBothConsentsTogether() {
		User user = localUser(UserConsents.unconsented());
		ReflectionTestUtils.setField(user, "consents", null);
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		UserConsentResponse response;
		try (LogCapture logs = LogCapture.forClass(UserConsentService.class)) {
			response = userConsentService.updateConsents(validRequest());
			assertThat(logs.events("user.consents.updated")).singleElement()
					.satisfies(event -> {
						assertThat(event.getFormattedMessage())
								.isEqualTo("사용자 동의를 갱신했습니다");
						assertThat(LogCapture.value(event, "outcome")).isEqualTo("updated");
						assertThat(LogCapture.value(event, "userId"))
								.isEqualTo(user.getUserId());
						assertThat(LogCapture.value(event, "accountType"))
								.isEqualTo(UserAccountType.MEMBER);
						assertThat(LogCapture.rendered(event)).doesNotContain(
								"user@example.com",
								"test-only-password-hash",
								"테스트사용자",
								QUALITY_REVIEW_VERSION,
								NOW.toString()
						);
					});
		}

		verify(currentUserProvider).getCurrentUserId();
		verify(userRepository).findById(user.getUserId());
		verify(userRepository).updateConsentsIfActive(user, PREVIOUS_AT);
		assertThat(response.privacyConsented()).isTrue();
		assertThat(response.privacyConsentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacyConsentedAt()).isEqualTo(NOW);
		assertThat(response.termConsented()).isTrue();
		assertThat(response.termConsentVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.termConsentedAt()).isEqualTo(NOW);
		assertThat(response.qualityReviewConsented()).isFalse();
		assertThat(response.qualityReviewConsentVersion()).isNull();
		assertThat(response.qualityReviewConsentedAt()).isNull();
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
		verify(userRepository, never()).updateConsentsIfActive(any(), any());
		assertThat(response.privacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.termConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(user.getUpdatedAt()).isEqualTo(previousUpdatedAt);
	}

	@Test
	void concurrentWithdrawalPreventsStaleConsentUpdateFromRestoringActiveUser() {
		User user = localUser(UserConsents.consented("privacy-v1", "term-v1", PREVIOUS_AT));
		User withdrawn = user.toWithdrawnTombstone(NOW.minusSeconds(1));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId()))
				.thenReturn(Optional.of(user), Optional.of(withdrawn));
		when(userRepository.updateConsentsIfActive(any(), any())).thenReturn(false);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(validRequest())
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		verify(userRepository).updateConsentsIfActive(user, PREVIOUS_AT);
		verify(userRepository, never()).save(any(User.class));
		assertThat(withdrawn.getStatus()).isEqualTo(
				web.tosunsaeng.identity.domain.user.domain.enums.UserStatus.WITHDRAWN
		);
	}

	@Test
	void changedVersionsUseServerTimeForBothConsents() {
		User user = localUser(UserConsents.consented("privacy-v1", "term-v1", PREVIOUS_AT));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
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
		UserConsentResponse response = userConsentService.updateConsents(validRequest());

		assertThat(response.privacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.termConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void grantsQualityReviewConsentWithCurrentVersionAndServerTime() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentResponse response = userConsentService.updateConsents(
				qualityReviewRequest(true, QUALITY_REVIEW_VERSION)
		);

		verify(userRepository).updateConsentsIfActive(user, PREVIOUS_AT);
		assertThat(response.qualityReviewConsented()).isTrue();
		assertThat(response.qualityReviewConsentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
		assertThat(response.qualityReviewConsentedAt()).isEqualTo(NOW);
		assertThat(response.privacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(response.termConsentedAt()).isEqualTo(PREVIOUS_AT);
	}

	@Test
	void withdrawsQualityReviewConsentAndClearsStoredMetadata() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				QUALITY_REVIEW_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentResponse response = userConsentService.updateConsents(
				qualityReviewRequest(false, "quality-review-stale")
		);

		verify(userRepository).updateConsentsIfActive(user, PREVIOUS_AT);
		assertThat(response.qualityReviewConsented()).isFalse();
		assertThat(response.qualityReviewConsentVersion()).isNull();
		assertThat(response.qualityReviewConsentedAt()).isNull();
	}

	@Test
	void falseWithStaleQualityReviewVersionIsIdempotent() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		UserConsentResponse response = userConsentService.updateConsents(
				qualityReviewRequest(false, "quality-review-v1")
		);

		verify(userRepository, never()).updateConsentsIfActive(any(), any());
		assertThat(response.qualityReviewConsented()).isFalse();
		assertThat(user.getUpdatedAt()).isEqualTo(PREVIOUS_AT);
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = "quality-review-v1")
	void rejectsTrueQualityReviewWithMissingOrStaleVersionBeforeLookingUpUser(String version) {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(
						qualityReviewRequest(true, version)
				)
		);

		assertThat(exception.getErrorCode()).isEqualTo(
				UserErrorStatus.QUALITY_REVIEW_CONSENT_VERSION_MISMATCH
		);
		verify(currentUserProvider, never()).getCurrentUserId();
	}

	@Test
	void oldQualityReviewVersionIsNotReportedAsCurrentConsent() {
		User user = localUser(UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				"quality-review-v1",
				PREVIOUS_AT
		));
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));

		ConsentPolicyStatusResponse status = userConsentService
				.getCurrentConsentStatus()
				.qualityReview();

		assertThat(status.consented()).isFalse();
		assertThat(status.consentedVersion()).isEqualTo("quality-review-v1");
		assertThat(status.consentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(status.requiresConsent()).isFalse();
	}

	@Test
	void rejectsFalsePrivacyConsentBeforeLookingUpUser() {
		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						false,
						PRIVACY_VERSION,
						true,
						TERM_VERSION,
						false,
						QUALITY_REVIEW_VERSION
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
						TERM_VERSION,
						false,
						QUALITY_REVIEW_VERSION
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
						TERM_VERSION,
						false,
						QUALITY_REVIEW_VERSION
				))
		);
		BusinessException termFailure = catchThrowableOfType(
				BusinessException.class,
				() -> userConsentService.updateConsents(new UserConsentUpdateRequest(
						true,
						PRIVACY_VERSION,
						true,
						"term-v1",
						false,
						QUALITY_REVIEW_VERSION
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
						"termConsentVersion",
						"isQualityReviewConsented",
						"qualityReviewConsentVersion"
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
				TERM_VERSION,
				false,
				QUALITY_REVIEW_VERSION
		);
	}

	private UserConsentUpdateRequest qualityReviewRequest(
			boolean consented,
			String version
	) {
		return new UserConsentUpdateRequest(
				true,
				PRIVACY_VERSION,
				true,
				TERM_VERSION,
				consented,
				version
		);
	}
}
