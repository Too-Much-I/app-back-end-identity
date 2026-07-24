package web.tosunsaeng.identity.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserFactory;
import web.tosunsaeng.identity.user.domain.UserStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

class AuthServiceTests {

	private static final String AUDIO_POLICY_VERSION = "test-audio-policy-v3";
	private static final String RAW_CREDENTIAL = "test-only-credential";

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = new BCryptPasswordEncoder(4);
		EmailNormalizer emailNormalizer = new EmailNormalizer();
		UserFactory userFactory = new UserFactory(
				emailNormalizer,
				passwordEncoder,
				AUDIO_POLICY_VERSION
		);
		authService = new AuthService(userRepository, emailNormalizer, userFactory);
	}

	@Test
	void returnsAvailableWhenNormalizedEmailDoesNotExist() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		CheckEmailResponse response = authService.checkEmail("user@example.com");

		assertThat(response.isAvailable()).isTrue();
		assertThat(response.message()).isEqualTo("사용 가능한 이메일입니다.");
	}

	@Test
	void returnsUnavailableNormallyWhenNormalizedEmailExists() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		CheckEmailResponse response = authService.checkEmail("user@example.com");

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.message()).isEqualTo("이미 사용 중인 이메일입니다.");
	}

	@Test
	void trimsEmailBeforeAvailabilityLookup() {
		authService.checkEmail("  user@example.com  ");

		verify(userRepository).existsByNormalizedEmail("user@example.com");
	}

	@Test
	void lowercasesEmailBeforeAvailabilityLookup() {
		authService.checkEmail("USER@EXAMPLE.COM");

		verify(userRepository).existsByNormalizedEmail("user@example.com");
	}

	@Test
	void createsActiveUserWithNormalizedEmailHashNicknameAndAudioConsent() {
		when(userRepository.existsByNormalizedEmail("sample.user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = authService.signup(new SignupRequest(
				"  Sample.User@EXAMPLE.COM  ",
				RAW_CREDENTIAL,
				"  토스마스터  ",
				true
		));

		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(userCaptor.capture());
		User savedUser = userCaptor.getValue();

		assertThat(savedUser.getNormalizedEmail()).isEqualTo("sample.user@example.com");
		assertThat(savedUser.getEmail()).isEqualTo("Sample.User@EXAMPLE.COM");
		assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_CREDENTIAL);
		assertThat(passwordEncoder.matches(RAW_CREDENTIAL, savedUser.getPasswordHash())).isTrue();
		assertThat(UUID.fromString(savedUser.getUserId()).toString()).isEqualTo(savedUser.getUserId());
		assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(savedUser.getNickname()).isEqualTo("토스마스터");
		assertThat(savedUser.getAudioConsent().isAgreed()).isTrue();
		assertThat(savedUser.getAudioConsent().getPolicyVersion()).isEqualTo(AUDIO_POLICY_VERSION);
		assertThat(savedUser.getAudioConsent().getAgreedAt()).isNotNull();
		assertThat(savedUser.getAudioConsent().getWithdrawnAt()).isNull();

		assertThat(response.userId()).isEqualTo(savedUser.getUserId());
		assertThat(response.email()).isEqualTo("Sample.User@EXAMPLE.COM");
		assertThat(response.nickname()).isEqualTo("토스마스터");
		assertThat(response.isAudioConsent()).isTrue();
		assertThat(response.createdAt()).isEqualTo(savedUser.getCreatedAt());
	}

	@Test
	void rejectsSignupWhenAudioConsentIsFalse() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> authService.signup(signupRequest(false))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.AUDIO_CONSENT_REQUIRED);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void rejectsSignupWhenEmailAlreadyExistsBeforeEncodingAndSaving() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> authService.signup(signupRequest(true))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void convertsDuplicateKeyOnSaveToEmailAlreadyExists() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenThrow(
				new DuplicateKeyException("database index detail that must stay internal")
		);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> authService.signup(signupRequest(true))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		assertThat(exception.getMessage())
				.isEqualTo("이미 사용 중인 이메일입니다.")
				.doesNotContain("database index detail");
	}

	private SignupRequest signupRequest(Boolean audioConsent) {
		return new SignupRequest(
				"user@example.com",
				RAW_CREDENTIAL,
				"테스트닉네임",
				audioConsent
		);
	}
}
