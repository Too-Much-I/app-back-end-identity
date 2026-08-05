package web.tosunsaeng.identity.domain.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.application.EmailAvailabilityService;
import web.tosunsaeng.identity.domain.auth.application.GuestAuthService;
import web.tosunsaeng.identity.domain.auth.application.LoginService;
import web.tosunsaeng.identity.domain.auth.application.LogoutService;
import web.tosunsaeng.identity.domain.auth.application.LogoutAllService;
import web.tosunsaeng.identity.domain.auth.application.SignupService;
import web.tosunsaeng.identity.domain.auth.application.TokenReissueService;
import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.global.config.PasswordConfig;
import web.tosunsaeng.identity.global.config.SecurityConfig;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.domain.auth.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		EmailAvailabilityService.class,
		SignupService.class,
		LoginService.class,
		TokenReissueService.class,
		LogoutService.class,
		AuthResponseConverter.class,
		ConsentPolicy.class,
		EmailNormalizer.class,
		UserFactory.class,
		PasswordConfig.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class,
		RefreshTokenHasher.class
})
@TestPropertySource(properties = {
		"app.consent.privacy-version=privacy-v1",
		"app.consent.term-version=term-v1"
})
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserFactory userFactory;

	@Autowired
	private RefreshTokenHasher refreshTokenHasher;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AccessTokenIssuer accessTokenIssuer;

	@MockitoBean
	private RefreshSessionIssuer refreshSessionIssuer;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@MockitoBean
	private Clock clock;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private LogoutAllService logoutAllService;

	@MockitoBean
	private GuestAuthService guestAuthService;

	@Test
	void checkEmailReturnsAvailableAndNormalizesWhitespaceAndCase() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		mockMvc.perform(post("/api/v1/auth/check-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"  User@Example.COM  \"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.isAvailable").value(true))
				.andExpect(jsonPath("$.result.message").value("사용 가능한 이메일입니다."));

		verify(userRepository).existsByNormalizedEmail("user@example.com");
	}

	@Test
	void checkEmailReturnsUnavailableAsSuccessfulResponse() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/check-email")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"user@example.com\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.result.isAvailable").value(false))
				.andExpect(jsonPath("$.result.message").value("이미 사용 중인 이메일입니다."));
	}

	@Test
	void signupReturnsSafeResponseAndPersistsRequiredFields() throws Exception {
		when(userRepository.existsByNormalizedEmail("sample.user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "  Sample.User@EXAMPLE.COM  ",
								  "password": "test-only-credential",
								  "nickname": "  토스마스터  ",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.userId").isNotEmpty())
				.andExpect(jsonPath("$.result.email").value("Sample.User@EXAMPLE.COM"))
				.andExpect(jsonPath("$.result.nickname").value("토스마스터"))
				.andExpect(jsonPath("$.result.privacyConsented").value(true))
				.andExpect(jsonPath("$.result.privacyConsentVersion").value("privacy-v1"))
				.andExpect(jsonPath("$.result.privacyConsentedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.termConsented").value(true))
				.andExpect(jsonPath("$.result.termConsentVersion").value("term-v1"))
				.andExpect(jsonPath("$.result.termConsentedAt").isNotEmpty())
				.andExpect(jsonPath("$.result.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.result.password").doesNotExist())
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.result.normalizedEmail").doesNotExist())
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist());
	}

	@Test
	void signupAcceptsMinimumLengthWithoutUnspecifiedComplexityRule() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("abcdefgh")))
				.andExpect(status().isOk());
	}

	@Test
	void signupRejectsFalsePrivacyConsent() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(
								"test-only-credential",
								false,
								"privacy-v1",
								true,
								"term-v1"
						)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("PRIVACY_CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.message").value("개인정보 처리 동의가 필요합니다."));
	}

	@Test
	void signupRejectsOldAudioOnlyContractAsValidationError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "user@example.com",
								  "password": "test-only-credential",
								  "nickname": "테스트닉네임",
								  "isAudioConsent": true
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[*].field",
						org.hamcrest.Matchers.hasItem("isPrivacyConsented")))
				.andExpect(jsonPath("$.result[*].field",
						org.hamcrest.Matchers.hasItem("privacyConsentVersion")))
				.andExpect(jsonPath("$.result[*].field",
						org.hamcrest.Matchers.hasItem("isTermConsented")))
				.andExpect(jsonPath("$.result[*].field",
						org.hamcrest.Matchers.hasItem("termConsentVersion")));
	}

	@Test
	void signupRejectsVersionThatDoesNotMatchServerPolicy() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(signupJson(
								"test-only-credential",
								true,
								"privacy-old",
								true,
								"term-v1"
						)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PRIVACY_CONSENT_VERSION_MISMATCH"));
	}

	@Test
	void signupReturnsConflictWhenEmailAlreadyExistsBeforeSave() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("test-only-credential")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
	}

	@Test
	void signupDoesNotExposeCredentialOrMongoDetailsOnDuplicateKey() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenThrow(
				new DuplicateKeyException("uk_users_normalized_email internal database detail")
		);

		MvcResult result = mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("test-only-credential")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("test-only-credential")
				.doesNotContain("uk_users_normalized_email")
				.doesNotContain("internal database detail");
	}

	@Test
	void signupRejectsInvalidEmailFormat() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invalid-email",
								  "password": "test-only-credential",
								  "nickname": "테스트닉네임",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("email"));
	}

	@Test
	void signupRejectsShortPasswordAndMasksRejectedValue() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("short")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("password"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));
	}

	@Test
	void signupRejectsBlankNicknameAfterTrimming() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "user@example.com",
								  "password": "test-only-credential",
								  "nickname": "   ",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("nickname"));
	}

	@Test
	void loginReturnsTokenResponseWithoutInternalUserOrSessionFields() throws Exception {
		String rawCredential = "controller-test-credential";
		User user = userFactory.create("User@Example.com", rawCredential, "로그인사용자");
		Instant issuedAt = Instant.parse("2026-07-24T05:06:07Z");
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.of(user));
		when(accessTokenIssuer.issue(user.getUserId(), Set.of())).thenReturn(new IssuedAccessToken(
				"test-access-value",
				"Bearer",
				issuedAt,
				issuedAt.plus(Duration.ofMinutes(30)),
				1_800
		));
		when(refreshSessionIssuer.issue(user.getUserId())).thenReturn(new IssuedRefreshSession(
				"test-refresh-value",
				issuedAt,
				issuedAt.plus(Duration.ofDays(14))
		));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "  USER@EXAMPLE.COM  ",
								  "password": "controller-test-credential"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.result.accessToken").value("test-access-value"))
				.andExpect(jsonPath("$.result.refreshToken").value("test-refresh-value"))
				.andExpect(jsonPath("$.result.grantType").value("Bearer"))
				.andExpect(jsonPath("$.result.accessTokenExpiresIn").value(1_800_000))
				.andExpect(jsonPath("$.result.password").doesNotExist())
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.result.normalizedEmail").doesNotExist())
				.andExpect(jsonPath("$.result.tokenHash").doesNotExist())
				.andExpect(jsonPath("$.result.sessionId").doesNotExist())
				.andReturn();

		verify(userRepository).findByNormalizedEmail("user@example.com");
		verify(accessTokenIssuer).issue(user.getUserId(), Set.of());
		verify(refreshSessionIssuer).issue(user.getUserId());
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(rawCredential, user.getPasswordHash(), user.getNormalizedEmail());
	}

	@Test
	void unknownEmailAndWrongPasswordExposeTheSameGenericUnauthorizedError() throws Exception {
		when(userRepository.findByNormalizedEmail("unknown@example.com"))
				.thenReturn(Optional.empty());

		MvcResult unknownEmailResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("unknown@example.com", "unknown-test-value")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andExpect(jsonPath("$.refreshToken").doesNotExist())
				.andReturn();

		User user = userFactory.create(
				"user@example.com",
				"stored-test-credential",
				"로그인사용자"
		);
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.of(user));

		MvcResult wrongPasswordResult = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("user@example.com", "wrong-test-value")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andExpect(jsonPath("$.refreshToken").doesNotExist())
				.andReturn();

		assertThat(unknownEmailResult.getResponse().getContentAsString())
				.isEqualTo(wrongPasswordResult.getResponse().getContentAsString())
				.doesNotContain(
						"unknown-test-value",
						"wrong-test-value",
						user.getPasswordHash(),
						"accessToken",
						"refreshToken"
				);
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionIssuer, never()).issue(any());
	}

	@ParameterizedTest
	@EnumSource(value = UserStatus.class, names = {"SUSPENDED", "WITHDRAWN"})
	void loginRejectsNonActiveAccountWithGenericForbiddenError(UserStatus status) throws Exception {
		String rawCredential = "account-status-test-value";
		User user = userFactory.create("user@example.com", rawCredential, "로그인사용자");
		ReflectionTestUtils.setField(user, "status", status);
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.of(user));

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("user@example.com", rawCredential)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"))
				.andExpect(jsonPath("$.message").value("활성 상태가 아닌 계정은 로그인할 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()));

		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionIssuer, never()).issue(any());
	}

	@Test
	void loginRejectsInvalidEmail() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("invalid-email", "validation-test-value")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("email"));
	}

	@Test
	void loginRejectsBlankPasswordAndMasksRejectedValue() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("user@example.com", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("password"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));
	}

	@Test
	void loginRejectsPasswordLongerThanSignupMaximumAndMasksRejectedValue() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("user@example.com", "a".repeat(65))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("password"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));
	}

	@Test
	void loginDoesNotReapplySignupMinimumLengthOrComplexityValidation() throws Exception {
		when(userRepository.findByNormalizedEmail("user@example.com"))
				.thenReturn(Optional.empty());

		mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(loginJson("user@example.com", "x")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void reissueReturnsBothTokensInMillisecondsWithoutInternalFields() throws Exception {
		String currentRefreshValue = "controller-current-refresh-value";
		Instant currentTime = Instant.parse("2026-07-27T04:05:06Z");
		User user = userFactory.create(
				"reissue.user@example.com",
				"controller-test-credential",
				"재발급사용자"
		);
		RefreshSession currentSession = RefreshSession.create(
				user.getUserId(),
				refreshTokenHasher.hash(currentRefreshValue),
				currentTime.minusSeconds(60),
				currentTime.plus(Duration.ofDays(14))
		);
		when(clock.instant()).thenReturn(currentTime);
		when(refreshSessionRepository.findByTokenHash(
				refreshTokenHasher.hash(currentRefreshValue)
		)).thenReturn(Optional.of(currentSession));
		when(refreshSessionRepository.save(currentSession)).thenReturn(currentSession);
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(accessTokenIssuer.issue(user.getUserId(), Set.of())).thenReturn(new IssuedAccessToken(
				"controller-next-access-value",
				"Bearer",
				currentTime,
				currentTime.plus(Duration.ofMinutes(30)),
				1_800
		));
		when(refreshSessionIssuer.issueRotated(any(), any(), any(), any(), any()))
				.thenReturn(new IssuedRefreshSession(
						"controller-next-refresh-value",
						currentTime,
						currentTime.plus(Duration.ofDays(14))
				));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson(currentRefreshValue)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.accessToken")
						.value("controller-next-access-value"))
				.andExpect(jsonPath("$.result.refreshToken")
						.value("controller-next-refresh-value"))
				.andExpect(jsonPath("$.result.grantType").value("Bearer"))
				.andExpect(jsonPath("$.result.accessTokenExpiresIn").value(1_800_000))
				.andExpect(jsonPath("$.result.refreshTokenExpiresIn").value(1_209_600_000))
				.andExpect(jsonPath("$.result.tokenHash").doesNotExist())
				.andExpect(jsonPath("$.result.sessionId").doesNotExist())
				.andExpect(jsonPath("$.result.userId").doesNotExist())
				.andExpect(jsonPath("$.result.rotationFamilyId").doesNotExist())
				.andExpect(jsonPath("$.result.passwordHash").doesNotExist())
				.andReturn();

		verify(refreshSessionRepository).findByTokenHash(
				refreshTokenHasher.hash(currentRefreshValue)
		);
		verify(refreshSessionRepository).save(currentSession);
		verify(refreshSessionIssuer).issueRotated(
				eq(currentSession.getReplacedBySessionId()),
				eq(user.getUserId()),
				eq(currentSession.getRotationFamilyId()),
				eq(currentSession.getSessionId()),
				eq(currentTime)
		);
		assertThat(currentSession.getRevocationReason()).isEqualTo(RevocationReason.ROTATED);
		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(
						currentRefreshValue,
						currentSession.getTokenHash(),
						currentSession.getSessionId(),
						currentSession.getRotationFamilyId(),
						user.getUserId(),
						user.getPasswordHash()
				);
	}

	@Test
	void reissueUnknownTokenReturnsGenericUnauthorizedWithoutEchoingToken() throws Exception {
		String unknownRefreshValue = "controller-unknown-refresh-value";
		when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		MvcResult result = mockMvc.perform(post("/api/v1/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson(unknownRefreshValue)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"))
				.andExpect(jsonPath("$.message").value("유효하지 않은 Refresh Token"))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain(unknownRefreshValue, refreshTokenHasher.hash(unknownRefreshValue));
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionIssuer, never()).issueRotated(any(), any(), any(), any(), any());
	}

	@Test
	void reissueValidationMasksBlankAndOversizedRefreshTokenValues() throws Exception {
		mockMvc.perform(post("/api/v1/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("refreshToken"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));

		mockMvc.perform(post("/api/v1/auth/reissue")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson("x".repeat(513))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("refreshToken"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));
	}

	@Test
	void logoutRevokesSessionAndReturnsNoTokenPayload() throws Exception {
		String refreshValue = "controller-logout-refresh-value";
		Instant currentTime = Instant.parse("2026-07-27T04:05:06Z");
		RefreshSession session = RefreshSession.create(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				refreshTokenHasher.hash(refreshValue),
				currentTime.minusSeconds(60),
				currentTime.plus(Duration.ofDays(14))
		);
		when(clock.instant()).thenReturn(currentTime);
		when(refreshSessionRepository.findByTokenHash(refreshTokenHasher.hash(refreshValue)))
				.thenReturn(Optional.of(session));
		when(refreshSessionRepository.save(session)).thenReturn(session);

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson(refreshValue)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.code").value("SUCCESS"))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.refreshToken").doesNotExist());

		assertThat(session.getRevokedAt()).isEqualTo(currentTime);
		assertThat(session.getLastUsedAt()).isEqualTo(currentTime);
		assertThat(session.getRevocationReason()).isEqualTo(RevocationReason.LOGOUT);
		verify(refreshSessionRepository).save(session);
		verify(accessTokenIssuer, never()).issue(any(), any());
		verify(refreshSessionIssuer, never()).issue(any());
		verify(refreshSessionIssuer, never()).issueRotated(any(), any(), any(), any(), any());
	}

	@Test
	void logoutUnknownTokenIsSuccessfulAndBlankTokenIsMasked() throws Exception {
		when(refreshSessionRepository.findByTokenHash(any())).thenReturn(Optional.empty());

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson("controller-missing-refresh-value")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result").value(nullValue()));

		mockMvc.perform(post("/api/v1/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content(refreshRequestJson("")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("refreshToken"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()));
	}

	@Test
	void logoutAllHasNoRequestBodyAndDelegatesWithoutReturningTokenOrSessionData() throws Exception {
		mockMvc.perform(post("/api/v1/auth/logout-all"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andExpect(jsonPath("$.sessionId").doesNotExist())
				.andExpect(jsonPath("$.tokenHash").doesNotExist())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.refreshToken").doesNotExist());

		verify(logoutAllService).logoutAll();
	}

	@Test
	void controllerDependsOnServiceRatherThanRepository() {
		Class<?>[] dependencyTypes = Arrays.stream(AuthController.class.getDeclaredFields())
				.map(Field::getType)
				.toArray(Class<?>[]::new);

		assertThat(dependencyTypes)
				.contains(
						EmailAvailabilityService.class,
						SignupService.class,
						LoginService.class,
						TokenReissueService.class,
						LogoutService.class,
						LogoutAllService.class
				)
				.doesNotContain(UserRepository.class);
	}

	private String validSignupJson(String password) {
		return signupJson(password, true, "privacy-v1", true, "term-v1");
	}

	private String signupJson(
			String password,
			boolean privacyConsented,
			String privacyVersion,
			boolean termConsented,
			String termVersion
	) {
		return """
				{
				  "email": "user@example.com",
				  "password": "%s",
				  "nickname": "테스트닉네임",
				  "isPrivacyConsented": %s,
				  "privacyConsentVersion": "%s",
				  "isTermConsented": %s,
				  "termConsentVersion": "%s"
				}
				""".formatted(
				password,
				privacyConsented,
				privacyVersion,
				termConsented,
				termVersion
		);
	}

	private String loginJson(String email, String password) {
		return """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
	}

	private String refreshRequestJson(String refreshToken) {
		return """
				{
				  "refreshToken": "%s"
				}
				""".formatted(refreshToken);
	}
}
