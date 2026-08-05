package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class UserProfileServiceTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-07-27T03:04:05Z");
	private static final String PRIVACY_VERSION = "privacy-v1";
	private static final String TERM_VERSION = "term-v1";

	private CurrentUserProvider currentUserProvider;
	private UserRepository userRepository;
	private UserProfileService userProfileService;

	@BeforeEach
	void setUp() {
		currentUserProvider = mock(CurrentUserProvider.class);
		userRepository = mock(UserRepository.class);
		userProfileService = new UserProfileService(currentUserProvider, userRepository);
		when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
	}

	@Test
	void looksUpJwtSubjectUserAndMapsSafeLocalProfile() {
		User user = user(UserStatus.ACTIVE, consented());
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		UserProfileResponse response = userProfileService.getCurrentUserProfile();

		verify(currentUserProvider).getCurrentUserId();
		verify(userRepository).findById(USER_ID);
		assertThat(response.userId()).isEqualTo(USER_ID);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("토스마스터");
		assertThat(response.provider()).isEqualTo(UserProvider.LOCAL);
		assertThat(response.privacyConsented()).isTrue();
		assertThat(response.privacyConsentVersion()).isEqualTo(PRIVACY_VERSION);
		assertThat(response.privacyConsentedAt()).isEqualTo(CREATED_AT);
		assertThat(response.termConsented()).isTrue();
		assertThat(response.termConsentVersion()).isEqualTo(TERM_VERSION);
		assertThat(response.termConsentedAt()).isEqualTo(CREATED_AT);
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void mapsLegacyMissingConsentsToFalseAndNullWithoutExposingInternals() {
		User user = user(UserStatus.ACTIVE, UserConsents.unconsented());
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		UserProfileResponse response = userProfileService.getCurrentUserProfile();

		assertThat(response.privacyConsented()).isFalse();
		assertThat(response.privacyConsentVersion()).isNull();
		assertThat(response.privacyConsentedAt()).isNull();
		assertThat(response.termConsented()).isFalse();
		assertThat(response.termConsentVersion()).isNull();
		assertThat(response.termConsentedAt()).isNull();
		assertThat(Arrays.stream(UserProfileResponse.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly(
						"userId",
						"email",
						"nickname",
						"provider",
						"privacyConsented",
						"privacyConsentVersion",
						"privacyConsentedAt",
						"termConsented",
						"termConsentVersion",
						"termConsentedAt",
						"createdAt"
				)
				.doesNotContain(
						"password",
						"passwordHash",
						"normalizedEmail",
						"installationId",
						"guestInstallationIdHash",
						"refreshSession",
						"tokenHash",
						"accessToken",
						"refreshToken",
						"streakDays"
				);
	}

	@Test
	void mapsActiveGuestToNullEmailProfileWithoutInstallationIdentity() {
		User guest = user(UserStatus.ACTIVE, consented());
		when(guest.getEmail()).thenReturn(null);
		when(guest.getNickname()).thenReturn("게스트");
		when(guest.getProvider()).thenReturn(UserProvider.GUEST);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(guest));

		UserProfileResponse response = userProfileService.getCurrentUserProfile();

		assertThat(response.userId()).isEqualTo(USER_ID);
		assertThat(response.email()).isNull();
		assertThat(response.nickname()).isEqualTo("게스트");
		assertThat(response.provider()).isEqualTo(UserProvider.GUEST);
		assertThat(response.privacyConsented()).isTrue();
		assertThat(response.termConsented()).isTrue();
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
		assertThat(Arrays.stream(UserProfileResponse.class.getRecordComponents())
				.map(RecordComponent::getName))
				.doesNotContain("installationId", "guestInstallationIdHash");
	}

	@Test
	void returnsUserNotFoundForUnknownJwtSubject() {
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				userProfileService::getCurrentUserProfile
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.USER_NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("사용자를 찾을 수 없습니다.");
	}

	@ParameterizedTest
	@EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
	void rejectsNonActiveProfileWithExistingAccountError(UserStatus status) {
		User user = user(status, consented());
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				userProfileService::getCurrentUserProfile
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		assertThat(exception.getErrorCode().getCode()).isEqualTo("ACCOUNT_NOT_ACTIVE");
		assertThat(exception.getErrorCode().getHttpStatus().value()).isEqualTo(403);
		assertThat(exception.getMessage()).isEqualTo("활성 상태가 아닌 계정은 로그인할 수 없습니다.");
	}

	private User user(UserStatus status, UserConsents consents) {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(USER_ID);
		when(user.getEmail()).thenReturn("user@example.com");
		when(user.getNickname()).thenReturn("토스마스터");
		when(user.getProvider()).thenReturn(UserProvider.LOCAL);
		when(user.getConsents()).thenReturn(consents);
		when(user.getStatus()).thenReturn(status);
		when(user.getCreatedAt()).thenReturn(CREATED_AT);
		return user;
	}

	private UserConsents consented() {
		return UserConsents.consented(PRIVACY_VERSION, TERM_VERSION, CREATED_AT);
	}
}
