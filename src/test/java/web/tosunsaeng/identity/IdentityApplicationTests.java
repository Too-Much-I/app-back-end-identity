package web.tosunsaeng.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
		IdentityApplicationTests.TestEndpointConfiguration.class,
		TestRsaKeyConfiguration.class
})
class IdentityApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void actuatorHealthIsAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void openApiDocsAreAvailableWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.openapi").exists());
	}

	@Test
	void openApiDefinesIdentityMetadataAndJwtBearerSchemeWithoutGlobalSecurity() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("토선생 Identity API"))
				.andExpect(jsonPath("$.info.version").value("1.0.0"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security").doesNotExist());
	}

	@Test
	void openApiMarksOnlyProtectedOperationsAsBearerAuthenticated() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/auth/check-email'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/signup'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/reissue'].post.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/auth/logout'].post.security").doesNotExist())
				.andExpect(jsonPath(
						"$.paths['/api/v1/auth/logout-all'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/users/me'].get.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/users/me/consents'].put.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/users/me/consents'].get.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/users/withdraw'].post.security[0].bearerAuth"
				).isArray())
				.andExpect(jsonPath(
						"$.components.schemas.SignupRequest.properties.password.format"
				).value("password"))
				.andExpect(jsonPath(
						"$.components.schemas.SignupRequest.properties.password.writeOnly"
				).value(true))
				.andExpect(jsonPath(
						"$.components.schemas.SignupRequest.properties.password.example"
				).doesNotExist());
	}

	@Test
	void openApiDocumentsProviderSpecificWithdrawalWithoutTargetSelectors() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.summary")
						.value("회원 탈퇴"))
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.description")
						.value(org.hamcrest.Matchers.containsString("LOCAL")))
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.description")
						.value(org.hamcrest.Matchers.containsString("GUEST")))
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.responses['200']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.responses['400']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.responses['401']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.responses['404']")
						.exists())
				.andExpect(jsonPath("$.paths['/api/v1/users/withdraw'].post.responses['409']")
						.exists())
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.required")
						.value(org.hamcrest.Matchers.contains("refreshToken")))
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.properties.refreshToken.writeOnly")
						.value(true))
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.properties.password.writeOnly")
						.value(true))
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.properties.userId")
						.doesNotExist())
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.properties.installationId")
						.doesNotExist())
				.andExpect(jsonPath("$.components.schemas.WithdrawRequest.properties.isConfirmed")
						.doesNotExist());
	}

	@Test
	void openApiDocumentsGuestRequestResponseErrorsAndRecoveryLimit() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.summary")
						.value("Guest 사용자 생성 및 인증"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.description")
						.value(org.hamcrest.Matchers.containsString("인증 수단이 아니므로")))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.description")
						.value(org.hamcrest.Matchers.containsString("복구할 수 없습니다")))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['200']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['400']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['400']"
						+ ".content['application/json'].examples.PRIVACY_CONSENT_REQUIRED.value.code")
						.value("PRIVACY_CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['400']"
						+ ".content['application/json'].examples.TERM_CONSENT_REQUIRED.value.code")
						.value("TERM_CONSENT_REQUIRED"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['400']"
						+ ".content['application/json'].examples"
						+ ".QUALITY_REVIEW_CONSENT_VERSION_MISMATCH.value.code")
						.value("QUALITY_REVIEW_CONSENT_VERSION_MISMATCH"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['400']"
						+ ".content['application/json'].examples.INVALID_REQUEST.value.code")
						.value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['409']"
						+ ".content['application/json'].examples.GUEST_ALREADY_EXISTS.value.code")
						.value("GUEST_ALREADY_EXISTS"))
				.andExpect(jsonPath("$.components.schemas.GuestAuthRequest.required")
						.value(org.hamcrest.Matchers.hasItems(
								"installationId",
								"isPrivacyConsented",
								"privacyConsentVersion",
								"isTermConsented",
								"termConsentVersion"
						)))
				.andExpect(jsonPath("$.components.schemas.GuestAuthRequest.required")
						.value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItems(
								"isQualityReviewConsented",
								"qualityReviewConsentVersion"
						))))
				.andExpect(jsonPath(
						"$.components.schemas.GuestAuthRequest.properties.isQualityReviewConsented"
				).exists())
				.andExpect(jsonPath(
						"$.components.schemas.GuestAuthRequest.properties.qualityReviewConsentVersion.maxLength"
				).value(100))
				.andExpect(jsonPath(
						"$.components.schemas.GuestAuthRequest.properties.isAudioConsent"
				).doesNotExist())
				.andExpect(jsonPath("$.components.schemas.GuestAuthRequest.properties.installationId.minLength")
						.value(36))
				.andExpect(jsonPath("$.components.schemas.GuestAuthRequest.properties.installationId.maxLength")
						.value(36))
				.andExpect(jsonPath("$.paths['/api/v1/auth/guest'].post.responses['200']"
						+ ".content['application/json'].example.result.refreshTokenExpiresIn")
						.value(1_209_600_000));
	}

	@Test
	void openApiDocumentsConsentStatusReadUpdateAndLegacyNullability() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/v1/users/me/consents'].get.summary")
						.value("정책 동의 상태 조회"))
				.andExpect(jsonPath("$.paths['/api/v1/users/me/consents'].get.requestBody")
						.doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/users/me/consents'].put.summary")
						.value("정책 동의 갱신"))
				.andExpect(jsonPath(
						"$.components.schemas.UserConsentStatusResponse.properties.privacy"
				).exists())
				.andExpect(jsonPath(
						"$.components.schemas.UserConsentStatusResponse.properties.terms"
				).exists())
				.andExpect(jsonPath(
						"$.components.schemas.UserConsentStatusResponse.properties.qualityReview"
				).exists())
				.andExpect(jsonPath(
						"$.components.schemas.ConsentPolicyStatusResponse.properties.currentVersion.type"
				).value("string"))
				.andExpect(jsonPath(
						"$.components.schemas.ConsentPolicyStatusResponse.properties.consented.type"
				).value("boolean"))
				.andExpect(jsonPath(
						"$.components.schemas.ConsentPolicyStatusResponse.properties.consentedVersion.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.ConsentPolicyStatusResponse.properties.consentedAt.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.ConsentPolicyStatusResponse.properties.requiresConsent.type"
				).value("boolean"))
				.andExpect(jsonPath("$.components.schemas.UserConsentUpdateRequest.required")
						.value(org.hamcrest.Matchers.hasItems(
								"isPrivacyConsented",
								"privacyConsentVersion",
								"isTermConsented",
								"termConsentVersion"
						)))
				.andExpect(jsonPath("$.components.schemas.UserConsentUpdateRequest.required")
						.value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItems(
								"isQualityReviewConsented",
								"qualityReviewConsentVersion"
						))))
				.andExpect(jsonPath(
						"$.components.schemas.UserConsentResponse.properties.qualityReviewConsentVersion.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserConsentResponse.properties.qualityReviewConsentedAt.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.qualityReviewConsented"
				).doesNotExist())
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.privacyConsentVersion.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.privacyConsentedAt.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.termConsentVersion.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.termConsentedAt.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.isAudioConsent"
				).doesNotExist());
	}

	@Test
	void openApiDeclaresProfileEmailAsStringOrNullForGuestResponses() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").value("3.1.0"))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.email.type"
				).value(org.hamcrest.Matchers.hasItems("string", "null")))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.email.format"
				).value("email"))
				.andExpect(jsonPath(
						"$.components.schemas.UserProfileResponse.properties.email.description"
				).value(org.hamcrest.Matchers.containsString("Guest 사용자는 null")));
	}

	@Test
	void unlistedEndpointRequiresAuthenticationWithoutLoginPageOrBasicChallenge() throws Exception {
		mockMvc.perform(get("/__test/open"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("COMMON_UNAUTHORIZED"));
	}

	@Test
	void validationErrorUsesBaseResponseAndMasksSensitiveRejectedValue() throws Exception {
		mockMvc.perform(post("/__test/validation")
						.with(jwt())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
				.andExpect(jsonPath("$.result[0].field").value("password"))
				.andExpect(jsonPath("$.result[0].rejectedValue").value(nullValue()))
				.andExpect(jsonPath("$.result[0].reason").isNotEmpty());
	}

	@Test
	void businessExceptionUsesConfiguredErrorCode() throws Exception {
		mockMvc.perform(get("/__test/business-error").with(jwt()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()));
	}

	@Test
	void unexpectedExceptionDoesNotExposeInternalDetailsOrStackTrace() throws Exception {
		MvcResult result = mockMvc.perform(get("/__test/unexpected-error").with(jwt()))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.isSuccess").value(false))
				.andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
				.andExpect(jsonPath("$.result").value(nullValue()))
				.andReturn();

		String responseBody = result.getResponse().getContentAsString();
		assertThat(responseBody)
				.doesNotContain("sensitive internal detail")
				.doesNotContain("IllegalStateException")
				.doesNotContain("stackTrace");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestEndpointConfiguration {

		@Bean
		TestEndpointController testEndpointController() {
			return new TestEndpointController();
		}
	}

	@RestController
	static class TestEndpointController {

		@GetMapping("/__test/open")
		Map<String, String> open() {
			return Map.of("status", "open");
		}

		@PostMapping("/__test/validation")
		void validate(@Valid @RequestBody ValidationRequest request) {
		}

		@GetMapping("/__test/business-error")
		void businessError() {
			throw new BusinessException(CommonErrorStatus.NOT_FOUND);
		}

		@GetMapping("/__test/unexpected-error")
		void unexpectedError() {
			throw new IllegalStateException("sensitive internal detail");
		}
	}

	record ValidationRequest(@NotBlank String password) {
	}
}
