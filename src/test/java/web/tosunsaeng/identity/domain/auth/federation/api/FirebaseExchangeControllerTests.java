package web.tosunsaeng.identity.domain.auth.federation.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthenticatedResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequiredResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequirement;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

@WebMvcTest(FirebaseExchangeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FirebaseExchangeControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FirebaseExchangeUseCase firebaseExchangeUseCase;

	@MockitoBean
	private FirebaseSignupUseCase firebaseSignupUseCase;

	@Test
	void returnsExplicitAuthenticatedResultWithoutInternalFirebaseFields() throws Exception {
		when(firebaseExchangeUseCase.exchange(new FirebaseExchangeRequest("request-only-token")))
				.thenReturn(new FirebaseAuthenticatedResponse(
						"identity-access",
						"identity-refresh",
						"Bearer",
						1_800_000,
						1_209_600_000
				));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/firebase/exchange")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"firebaseIdToken\":\"request-only-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.type").value("AUTHENTICATED"))
				.andExpect(jsonPath("$.result.accessToken").value("identity-access"))
				.andExpect(jsonPath("$.result.refreshToken").value("identity-refresh"))
				.andExpect(jsonPath("$.result.grantType").value("Bearer"))
				.andExpect(jsonPath("$.result.accessTokenExpiresIn").value(1_800_000))
				.andExpect(jsonPath("$.result.refreshTokenExpiresIn").value(1_209_600_000))
				.andExpect(jsonPath("$.result.firebaseUid").doesNotExist())
				.andExpect(jsonPath("$.result.firebaseProjectId").doesNotExist())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain("request-only-token");
	}

	@Test
	void returnsExplicitEnrollmentRequiredResult() throws Exception {
		when(firebaseExchangeUseCase.exchange(new FirebaseExchangeRequest("request-only-token")))
				.thenReturn(new FirebaseEnrollmentRequiredResponse(
						"550e8400-e29b-41d4-a716-446655440000",
						Set.of(
								FirebaseEnrollmentRequirement.PHONE_VERIFICATION,
								FirebaseEnrollmentRequirement.PROFILE,
								FirebaseEnrollmentRequirement.CONSENTS
						),
						600_000
				));

		mockMvc.perform(post("/api/v1/auth/firebase/exchange")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"firebaseIdToken\":\"request-only-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.type").value("ENROLLMENT_REQUIRED"))
				.andExpect(jsonPath("$.result.enrollmentId")
						.value("550e8400-e29b-41d4-a716-446655440000"))
				.andExpect(jsonPath("$.result.missingRequirements", hasItems(
						"PHONE_VERIFICATION",
						"PROFILE",
						"CONSENTS"
				)))
				.andExpect(jsonPath("$.result.expiresIn").value(600_000))
				.andExpect(jsonPath("$.result.accessToken").doesNotExist())
				.andExpect(jsonPath("$.result.refreshToken").doesNotExist());
	}

	@Test
	void unavailableErrorDoesNotEchoFirebaseCredential() throws Exception {
		when(firebaseExchangeUseCase.exchange(new FirebaseExchangeRequest("request-only-token")))
				.thenThrow(new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/firebase/exchange")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"firebaseIdToken\":\"request-only-token\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("FIREBASE_UNAVAILABLE"))
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain("request-only-token");
	}

	@Test
	void requestToStringRedactsCredential() {
		assertThat(new FirebaseExchangeRequest("request-only-token").toString())
				.isEqualTo("FirebaseExchangeRequest[firebaseIdToken=redacted]")
				.doesNotContain("request-only-token");
		assertThat(new FirebaseAuthenticatedResponse(
				"identity-access",
				"identity-refresh",
				"Bearer",
				1,
				1
		).toString()).doesNotContain("identity-access", "identity-refresh");
	}

	@Test
	void firebaseSignupReturnsOnlyIdentityTokensAndNeverEchoesProofs() throws Exception {
		FirebaseSignupRequest request = new FirebaseSignupRequest(
				"550e8400-e29b-41d4-a716-446655440000",
				"fresh-firebase-credential",
				"테스트회원",
				true,
				"privacy-v1",
				true,
				"term-v1"
		);
		when(firebaseSignupUseCase.signup(request)).thenReturn(new FirebaseSignupResponse(
				"identity-access",
				"identity-refresh",
				"Bearer",
				1_800_000,
				1_209_600_000
		));

		MvcResult result = mockMvc.perform(post("/api/v1/auth/firebase/signup")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
								  "firebaseIdToken": "fresh-firebase-credential",
								  "nickname": "테스트회원",
								  "isPrivacyConsented": true,
								  "privacyConsentVersion": "privacy-v1",
								  "isTermConsented": true,
								  "termConsentVersion": "term-v1"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.result.accessToken").value("identity-access"))
				.andExpect(jsonPath("$.result.refreshToken").value("identity-refresh"))
				.andExpect(jsonPath("$.result.firebaseUid").doesNotExist())
				.andExpect(jsonPath("$.result.userId").doesNotExist())
				.andExpect(jsonPath("$.result.phone").doesNotExist())
				.andReturn();

		assertThat(result.getResponse().getContentAsString())
				.doesNotContain("fresh-firebase-credential", "테스트회원");
		assertThat(request.toString())
				.doesNotContain("fresh-firebase-credential", "테스트회원", request.enrollmentId());
	}
}
