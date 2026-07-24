package web.tosunsaeng.identity.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.auth.service.AuthService;
import web.tosunsaeng.identity.common.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.config.PasswordConfig;
import web.tosunsaeng.identity.config.SecurityConfig;
import web.tosunsaeng.identity.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.security.refresh.IssuedRefreshSession;
import web.tosunsaeng.identity.security.refresh.RefreshSessionIssuer;
import web.tosunsaeng.identity.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserFactory;
import web.tosunsaeng.identity.user.domain.UserStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
		AuthService.class,
		EmailNormalizer.class,
		UserFactory.class,
		PasswordConfig.class,
		SecurityConfig.class,
		GlobalExceptionHandler.class
})
@TestPropertySource(properties = "app.consent.audio-policy-version=controller-test-policy-v1")
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserFactory userFactory;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private AccessTokenIssuer accessTokenIssuer;

	@MockitoBean
	private RefreshSessionIssuer refreshSessionIssuer;

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
								  "isAudioConsent": true
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.isSuccess").value(true))
				.andExpect(jsonPath("$.result.userId").isNotEmpty())
				.andExpect(jsonPath("$.result.email").value("Sample.User@EXAMPLE.COM"))
				.andExpect(jsonPath("$.result.nickname").value("토스마스터"))
				.andExpect(jsonPath("$.result.isAudioConsent").value(true))
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
						.content(validSignupJson("abcdefgh", true)))
				.andExpect(status().isOk());
	}

	@Test
	void signupRejectsFalseAudioConsent() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("test-only-credential", false)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("AUDIO_CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.message").value("음성 데이터 수집·이용 동의가 필요합니다."));
	}

	@Test
	void signupRejectsMissingAudioConsentAsValidationError() throws Exception {
		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "user@example.com",
								  "password": "test-only-credential",
								  "nickname": "테스트닉네임"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.result[0].field").value("isAudioConsent"));
	}

	@Test
	void signupReturnsConflictWhenEmailAlreadyExistsBeforeSave() throws Exception {
		when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

		mockMvc.perform(post("/api/v1/auth/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content(validSignupJson("test-only-credential", true)))
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
						.content(validSignupJson("test-only-credential", true)))
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
								  "isAudioConsent": true
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
						.content(validSignupJson("short", true)))
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
								  "isAudioConsent": true
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
	void controllerDependsOnServiceRatherThanRepository() {
		assertThat(Arrays.stream(AuthController.class.getDeclaredFields()).map(Field::getType))
				.contains(AuthService.class)
				.doesNotContain(UserRepository.class);
	}

	private String validSignupJson(String password, boolean isAudioConsent) {
		return """
				{
				  "email": "user@example.com",
				  "password": "%s",
				  "nickname": "테스트닉네임",
				  "isAudioConsent": %s
				}
				""".formatted(password, isAudioConsent);
	}

	private String loginJson(String email, String password) {
		return """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
	}
}
