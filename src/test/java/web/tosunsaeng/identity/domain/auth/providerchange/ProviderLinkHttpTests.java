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
				.andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("PREPARED"))
				.andExpect(jsonPath("$.result.linkAllowed").value(false))
				.andExpect(header().string("Cache-Control", "no-store")).andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain("test-proof", "accessToken", "refreshToken");
		verify(service).prepare(eq("owner"), eq(SocialProvider.GOOGLE), eq("test-proof"), eq(java.util.List.of("request")));
	}
	@Test void statusRequiresAuthenticatedOwnerAndFirebaseProof() throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/link/status").contentType("application/json")
				.content("{\"requestId\":\"request\",\"firebaseIdToken\":\"proof\"}")).andExpect(status().isOk());
		verify(current).getCurrentUserId(); verify(service).status("owner", "request", "proof");
	}
	@ParameterizedTest @ValueSource(strings = {"start", "complete"})
	void attemptOperationsRequireProofAndDelegateOwner(String action) throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/link/" + action).contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\"}")).andExpect(status().isBadRequest());
		verifyNoInteractions(service);
		mvc.perform(post("/api/v1/auth/firebase/providers/link/" + action).contentType("application/json")
				.content("{\"linkAttemptId\":\"attempt\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
		if (action.equals("start")) verify(service).start("owner", "attempt", "proof");
		else verify(service).complete("owner", "attempt", "proof");
		assertThat(new ProviderLinkController.AttemptRequest("id", "test-secret").toString()).doesNotContain("test-secret");
	}
	@Test void disabledCommonServiceFailsClosed() throws Exception {
		when(provider.getIfAvailable()).thenReturn(null);
		mvc.perform(post("/api/v1/auth/firebase/providers/link/prepare").contentType("application/json")
				.content("{\"provider\":\"GOOGLE\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PROVIDER_CHANGE_UNAVAILABLE"));
	}
}
