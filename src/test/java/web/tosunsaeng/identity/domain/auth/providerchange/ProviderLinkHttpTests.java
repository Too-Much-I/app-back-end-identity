package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueNoStoreFilter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

class ProviderLinkHttpTests {
	ProviderLinkService service; ObjectProvider<ProviderLinkService> provider; CurrentUserProvider current; MockMvc mvc;
	@BeforeEach void setup() {
		service = mock(ProviderLinkService.class); provider = mock(ObjectProvider.class); current = mock(CurrentUserProvider.class);
		when(provider.getIfAvailable()).thenReturn(service); when(current.getCurrentUserId()).thenReturn("owner");
		mvc = MockMvcBuilders.standaloneSetup(new ProviderLinkController(provider, current)).setControllerAdvice(new GlobalExceptionHandler())
				.addFilters(new ReissueNoStoreFilter(), new ProviderChangeRequestFilter()).build();
	}
	@Test void commonPrepareReturnsSafeStatusAndForwardsRequestKey() throws Exception {
		when(service.prepare(any(), any(), any(), any())).thenReturn(new ProviderLinkService.Status("attempt", SocialProvider.GOOGLE, "PREPARED", Instant.EPOCH, false));
		var result = mvc.perform(post("/api/v1/auth/firebase/providers/link/prepare").header("Idempotency-Key", "request")
				.contentType("application/json").content("{\"provider\":\"GOOGLE\",\"firebaseIdToken\":\"test-proof\"}"))
				.andExpect(status().isGone()).andExpect(jsonPath("$.code").value("PROVIDER_LINK_RETIRED"))
				.andExpect(header().string("Cache-Control", "no-store")).andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain("test-proof", "accessToken", "refreshToken");
		verifyNoInteractions(service, current);
	}
	@Test void statusRequiresAuthenticatedOwnerAndFirebaseProof() throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/link/status").contentType("application/json")
				.content("{\"requestId\":\"request\",\"firebaseIdToken\":\"proof\"}")).andExpect(status().isGone());
		verifyNoInteractions(service, current);
	}
	@ParameterizedTest @ValueSource(strings = {"start", "complete"})
	void attemptOperationsRequireProofAndDelegateOwner(String action) throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/link/" + action).contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\"}")).andExpect(status().isBadRequest());
		verifyNoInteractions(service);
		mvc.perform(post("/api/v1/auth/firebase/providers/link/" + action).contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isGone()).andExpect(header().string("Cache-Control", "no-store"));
		verifyNoInteractions(service, current);
		assertThat(new ProviderLinkController.AttemptRequest("id", "test-secret").toString()).doesNotContain("test-secret");
	}
	@Test void disabledCommonServiceFailsClosed() throws Exception {
		when(provider.getIfAvailable()).thenReturn(null);
		mvc.perform(post("/api/v1/auth/firebase/providers/link/prepare").contentType("application/json")
				.content("{\"provider\":\"GOOGLE\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isGone()).andExpect(jsonPath("$.code").value("PROVIDER_LINK_RETIRED"));
	}
	@Test void recoveryEndpointsValidateProofAndNeverAcceptUnknownFailureCode() throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/link/cancel").contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isGone()).andExpect(header().string("Cache-Control", "no-store"));
		mvc.perform(post("/api/v1/auth/firebase/providers/link/pending").contentType("application/json")
				.content("{\"firebaseIdToken\":\"proof\"}")).andExpect(status().isGone());
		mvc.perform(post("/api/v1/auth/firebase/providers/link/failure-report").contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\",\"firebaseIdToken\":\"proof\",\"failureCode\":\"CREDENTIAL_ALREADY_IN_USE\"}"))
				.andExpect(status().isGone());
		verifyNoInteractions(service, current);
		mvc.perform(post("/api/v1/auth/firebase/providers/link/failure-report").contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\",\"firebaseIdToken\":\"proof\",\"failureCode\":\"untrusted-message\"}"))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/auth/firebase/providers/link/pending").contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest());
		assertThat(new ProviderLinkController.ProofRequest("secret").toString()).doesNotContain("secret");
	}
}
