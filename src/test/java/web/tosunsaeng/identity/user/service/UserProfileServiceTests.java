package web.tosunsaeng.identity.user.service;

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

import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.user.domain.AudioConsent;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserProvider;
import web.tosunsaeng.identity.user.domain.UserStatus;
import web.tosunsaeng.identity.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

class UserProfileServiceTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant CREATED_AT = Instant.parse("2026-07-27T03:04:05Z");

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
		User user = user(UserStatus.ACTIVE, true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		UserProfileResponse response = userProfileService.getCurrentUserProfile();

		verify(currentUserProvider).getCurrentUserId();
		verify(userRepository).findById(USER_ID);
		assertThat(response.userId()).isEqualTo(USER_ID);
		assertThat(response.email()).isEqualTo("user@example.com");
		assertThat(response.nickname()).isEqualTo("토스마스터");
		assertThat(response.provider()).isEqualTo(UserProvider.LOCAL);
		assertThat(response.isAudioConsent()).isTrue();
		assertThat(response.createdAt()).isEqualTo(CREATED_AT);
	}

	@Test
	void mapsWithdrawnAudioConsentToFalseWithoutExposingConsentInternals() {
		User user = user(UserStatus.ACTIVE, false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		UserProfileResponse response = userProfileService.getCurrentUserProfile();

		assertThat(response.isAudioConsent()).isFalse();
		assertThat(Arrays.stream(UserProfileResponse.class.getRecordComponents())
				.map(RecordComponent::getName))
				.containsExactly(
						"userId",
						"email",
						"nickname",
						"provider",
						"isAudioConsent",
						"createdAt"
				)
				.doesNotContain(
						"password",
						"passwordHash",
						"normalizedEmail",
						"refreshSession",
						"tokenHash",
						"accessToken",
						"refreshToken",
						"streakDays"
				);
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
		User user = user(status, true);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				userProfileService::getCurrentUserProfile
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.ACCOUNT_NOT_ACTIVE);
	}

	private User user(UserStatus status, boolean audioConsentAgreed) {
		User user = mock(User.class);
		AudioConsent audioConsent = mock(AudioConsent.class);
		when(audioConsent.isAgreed()).thenReturn(audioConsentAgreed);
		when(user.getUserId()).thenReturn(USER_ID);
		when(user.getEmail()).thenReturn("user@example.com");
		when(user.getNickname()).thenReturn("토스마스터");
		when(user.getProvider()).thenReturn(UserProvider.LOCAL);
		when(user.getAudioConsent()).thenReturn(audioConsent);
		when(user.getStatus()).thenReturn(status);
		when(user.getCreatedAt()).thenReturn(CREATED_AT);
		return user;
	}
}
