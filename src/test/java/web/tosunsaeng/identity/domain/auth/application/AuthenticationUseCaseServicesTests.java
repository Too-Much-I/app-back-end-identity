package web.tosunsaeng.identity.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class AuthenticationUseCaseServicesTests {

	private static final String PRIVACY_CONSENT_VERSION = "privacy-v1";
	private static final String TERM_CONSENT_VERSION = "term-v1";
	private static final String RAW_CREDENTIAL = "test-only-credential";
	private static final Instant NOW = Instant.parse("2026-07-24T03:04:05Z");
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

	private UserRepository userRepository;
	private PasswordEncoder passwordEncoder;
	private UserFactory userFactory;
	private ConsentPolicy consentPolicy;
	private AccessTokenIssuer accessTokenIssuer;
	private RefreshSessionRepository refreshSessionRepository;
	private RefreshTokenHasher refreshTokenHasher;
	private EmailAvailabilityService emailAvailabilityService;
	private SignupService signupService;
	private LoginService loginService;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		passwordEncoder = spy(new BCryptPasswordEncoder(4));
		EmailNormalizer emailNormalizer = new EmailNormalizer();
		consentPolicy = new ConsentPolicy(PRIVACY_CONSENT_VERSION, TERM_CONSENT_VERSION);
		userFactory = new UserFactory(
				emailNormalizer,
				passwordEncoder,
				consentPolicy
		);
		accessTokenIssuer = mock(AccessTokenIssuer.class);
		refreshSessionRepository = mock(RefreshSessionRepository.class);
		refreshTokenHasher = new RefreshTokenHasher();
		RefreshTokenProperties refreshTokenProperties = new RefreshTokenProperties(
				REFRESH_TOKEN_TTL,
				32
		);
		RefreshSessionIssuer refreshSessionIssuer = new RefreshSessionIssuer(
				new RefreshTokenGenerator(refreshTokenProperties, new SecureRandom()),
				refreshTokenHasher,
				refreshSessionRepository,
				refreshTokenProperties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		when(refreshSessionRepository.save(any(RefreshSession.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		AuthResponseConverter authResponseConverter = new AuthResponseConverter();
		emailAvailabilityService = new EmailAvailabilityService(
				userRepository,
				emailNormalizer
		);
		signupService = new SignupService(
				userRepository,
				emailNormalizer,
				userFactory,
				consentPolicy
		);
		loginService = new LoginService(
				userRepository,
				emailNormalizer,
				passwordEncoder,
				accessTokenIssuer,
				refreshSessionIssuer,
				authResponseConverter
		);
	}

	@Test
	void returnsAvailableWhenNormalizedEmailDoesNotExist() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		CheckEmailResponse response = emailAvailabilityService.checkEmail("user@example.com");

		assertThat(response.isAvailable()).isTrue();
		assertThat(response.message()).isEqualTo("사용 가능한 이메일입니다.");
	}

	@Test
	void returnsUnavailableNormallyWhenNormalizedEmailExists() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		CheckEmailResponse response = emailAvailabilityService.checkEmail("user@example.com");

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.message()).isEqualTo("이미 사용 중인 이메일입니다.");
	}

	@Test
	void trimsEmailBeforeAvailabilityLookup() {
		emailAvailabilityService.checkEmail("  user@example.com  ");

		verify(userRepository).existsByNormalizedEmail("user@example.com");
	}

	@Test
	void lowercasesEmailBeforeAvailabilityLookup() {
		emailAvailabilityService.checkEmail("USER@EXAMPLE.COM");

		verify(userRepository).existsByNormalizedEmail("user@example.com");
	}

	@Test
	void createsActiveUserWithNormalizedEmailHashNicknameAndBothConsents() {
		when(userRepository.existsByNormalizedEmail("sample.user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		SignupResponse response = signupService.signup(new SignupRequest(
				"  Sample.User@EXAMPLE.COM  ",
				RAW_CREDENTIAL,
				"  토스마스터  ",
				true,
				PRIVACY_CONSENT_VERSION,
				true,
				TERM_CONSENT_VERSION
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
		assertThat(savedUser.getConsents().isPrivacyConsented()).isTrue();
		assertThat(savedUser.getConsents().getPrivacyConsentVersion())
				.isEqualTo(PRIVACY_CONSENT_VERSION);
		assertThat(savedUser.getConsents().getPrivacyConsentedAt())
				.isEqualTo(savedUser.getCreatedAt());
		assertThat(savedUser.getConsents().isTermConsented()).isTrue();
		assertThat(savedUser.getConsents().getTermConsentVersion())
				.isEqualTo(TERM_CONSENT_VERSION);
		assertThat(savedUser.getConsents().getTermConsentedAt())
				.isEqualTo(savedUser.getCreatedAt());

		assertThat(response.userId()).isEqualTo(savedUser.getUserId());
		assertThat(response.email()).isEqualTo("Sample.User@EXAMPLE.COM");
		assertThat(response.nickname()).isEqualTo("토스마스터");
		assertThat(response.privacyConsented()).isTrue();
		assertThat(response.privacyConsentVersion()).isEqualTo(PRIVACY_CONSENT_VERSION);
		assertThat(response.privacyConsentedAt()).isEqualTo(savedUser.getCreatedAt());
		assertThat(response.termConsented()).isTrue();
		assertThat(response.termConsentVersion()).isEqualTo(TERM_CONSENT_VERSION);
		assertThat(response.termConsentedAt()).isEqualTo(savedUser.getCreatedAt());
		assertThat(response.createdAt()).isEqualTo(savedUser.getCreatedAt());
	}

	@Test
	void rejectsSignupWhenPrivacyConsentIsFalse() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> signupService.signup(signupRequest(false))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.PRIVACY_CONSENT_REQUIRED);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void rejectsSignupWhenTermVersionDoesNotMatchCurrentPolicy() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> signupService.signup(new SignupRequest(
						"user@example.com",
						RAW_CREDENTIAL,
						"테스트닉네임",
						true,
						PRIVACY_CONSENT_VERSION,
						true,
						"term-old"
				))
		);

		assertThat(exception.getErrorCode())
				.isEqualTo(UserErrorStatus.TERM_CONSENT_VERSION_MISMATCH);
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void rejectsSignupWhenEmailAlreadyExistsBeforeEncodingAndSaving() {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> signupService.signup(signupRequest(true))
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
				() -> signupService.signup(signupRequest(true))
		);

		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		assertThat(exception.getMessage())
				.isEqualTo("이미 사용 중인 이메일입니다.")
				.doesNotContain("database index detail");
	}

	@Test
	void logsInActiveUserWithNormalizedEmailAndPersistsOnlyRefreshTokenHash() {
		User user = userFactory.create(
				"Login.User@example.com",
				RAW_CREDENTIAL,
				"로그인사용자"
		);
		when(userRepository.findByNormalizedEmail("login.user@example.com"))
				.thenReturn(Optional.of(user));
		IssuedAccessToken issuedAccessToken = new IssuedAccessToken(
				"test-access-value",
				"Bearer",
				NOW,
				NOW.plus(Duration.ofMinutes(30)),
				1_800
		);
		when(accessTokenIssuer.issue(user.getUserId(), Set.of()))
				.thenReturn(issuedAccessToken);

		LoginResponse response = loginService.login(new LoginRequest(
				"  LOGIN.User@EXAMPLE.COM  ",
				RAW_CREDENTIAL
		));

		verify(userRepository).findByNormalizedEmail("login.user@example.com");
		verify(passwordEncoder).matches(RAW_CREDENTIAL, user.getPasswordHash());
		verify(accessTokenIssuer).issue(user.getUserId(), Set.of());

		ArgumentCaptor<RefreshSession> sessionCaptor = ArgumentCaptor.forClass(RefreshSession.class);
		verify(refreshSessionRepository).save(sessionCaptor.capture());
		RefreshSession savedSession = sessionCaptor.getValue();

		assertThat(savedSession.getUserId()).isEqualTo(user.getUserId());
		assertThat(UUID.fromString(savedSession.getSessionId()).toString())
				.isEqualTo(savedSession.getSessionId());
		assertThat(savedSession.getTokenHash())
				.isNotEqualTo(response.refreshToken())
				.isEqualTo(refreshTokenHasher.hash(response.refreshToken()));
		assertThat(response.refreshToken())
				.matches("[A-Za-z0-9_-]+")
				.doesNotContain("=", ".");
		assertThat(savedSession.getCreatedAt()).isEqualTo(NOW);
		assertThat(savedSession.getLastUsedAt()).isEqualTo(NOW);
		assertThat(savedSession.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TOKEN_TTL));
		assertThat(savedSession.getRevokedAt()).isNull();
		assertThat(UUID.fromString(savedSession.getRotationFamilyId()).toString())
				.isEqualTo(savedSession.getRotationFamilyId());
		assertThat(savedSession.getRotatedFromSessionId()).isNull();
		assertThat(savedSession.getReplacedBySessionId()).isNull();
		assertThat(savedSession.getRevocationReason()).isNull();
		assertThat(response.accessToken()).isEqualTo(issuedAccessToken.tokenValue());
		assertThat(response.grantType()).isEqualTo("Bearer");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(1_800_000L);
		assertThat(response.toString())
				.contains("accessToken=redacted", "refreshToken=redacted")
				.doesNotContain(response.accessToken(), response.refreshToken());
	}

	@Test
	void unknownEmailAndWrongPasswordReturnSameInvalidCredentialsErrorWithoutIssuingTokens() {
		when(userRepository.findByNormalizedEmail("unknown@example.com"))
				.thenReturn(Optional.empty());

		BusinessException unknownEmailException = catchThrowableOfType(
				BusinessException.class,
				() -> loginService.login(new LoginRequest("unknown@example.com", RAW_CREDENTIAL))
		);

		User user = userFactory.create("user@example.com", RAW_CREDENTIAL, "로그인사용자");
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.of(user));
		BusinessException wrongPasswordException = catchThrowableOfType(
				BusinessException.class,
				() -> loginService.login(new LoginRequest("user@example.com", "different-value"))
		);

		assertThat(unknownEmailException.getErrorCode())
				.isEqualTo(AuthErrorStatus.INVALID_CREDENTIALS)
				.isEqualTo(wrongPasswordException.getErrorCode());
		assertThat(unknownEmailException.getMessage())
				.isEqualTo("이메일 또는 비밀번호가 올바르지 않습니다.")
				.isEqualTo(wrongPasswordException.getMessage());
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionRepository, never()).save(any(RefreshSession.class));
	}

	@ParameterizedTest
	@EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
	void rejectsNonActiveAccountWithoutIssuingTokens(UserStatus status) {
		User user = userFactory.create("user@example.com", RAW_CREDENTIAL, "로그인사용자");
		ReflectionTestUtils.setField(user, "status", status);
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.of(user));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> loginService.login(new LoginRequest("user@example.com", RAW_CREDENTIAL))
		);

		assertThat(exception.getErrorCode()).isEqualTo(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		assertThat(exception.getMessage()).isEqualTo("활성 상태가 아닌 계정은 로그인할 수 없습니다.");
		verify(passwordEncoder).matches(RAW_CREDENTIAL, user.getPasswordHash());
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionRepository, never()).save(any(RefreshSession.class));
	}

	private SignupRequest signupRequest(Boolean privacyConsented) {
		return new SignupRequest(
				"user@example.com",
				RAW_CREDENTIAL,
				"테스트닉네임",
				privacyConsented,
				PRIVACY_CONSENT_VERSION,
				true,
				TERM_CONSENT_VERSION
		);
	}
}
