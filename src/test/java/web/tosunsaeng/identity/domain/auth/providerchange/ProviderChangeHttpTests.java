package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueNoStoreFilter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

class ProviderChangeHttpTests {
	ProviderChangeService service; ObjectProvider<ProviderChangeService> provider; CurrentUserProvider current; MockMvc mvc;
	@BeforeEach void setup() {
		service = mock(ProviderChangeService.class); provider = mock(ObjectProvider.class); current = mock(CurrentUserProvider.class);
		when(provider.getIfAvailable()).thenReturn(service); when(current.getCurrentUserId()).thenReturn("user");
		mvc = MockMvcBuilders.standaloneSetup(new ProviderChangeController(provider, current)).setControllerAdvice(new GlobalExceptionHandler())
				.addFilters(new ReissueNoStoreFilter(), new ProviderChangeRequestFilter()).build();
	}
	@Test void acceptedIsNotCompletionAndHasNoCredential() throws Exception {
		when(service.unlink(any(), any(), any(), any())).thenReturn(new ProviderChangeService.Status("operation", SocialProvider.GOOGLE, "PROCESSING", Instant.EPOCH, null, 3));
		var result = mvc.perform(post("/api/v1/auth/firebase/providers/unlink").header("Idempotency-Key", "request")
				.contentType("application/json").content("{\"provider\":\"GOOGLE\",\"firebaseIdToken\":\"test-proof\"}"))
				.andExpect(status().isAccepted()).andExpect(jsonPath("$.result.status").value("PROCESSING"))
				.andExpect(header().string("Cache-Control", "no-store")).andReturn();
		assertThat(result.getResponse().getContentAsString()).doesNotContain("test-proof", "accessToken", "refreshToken");
	}
	@Test void statusDoesNotNeedCurrentUserButAlwaysDelegatesProofVerification() throws Exception {
		when(service.status(any(), any())).thenThrow(new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN));
		mvc.perform(post("/api/v1/auth/firebase/providers/unlink/status").contentType("application/json")
				.content("{\"requestId\":\"request\",\"firebaseIdToken\":\"bad-proof\"}"))
				.andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
		verifyNoInteractions(current); verify(service).status("request", "bad-proof");
	}
	@Test void missingProofIsRejected() throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/unlink/status").contentType("application/json").content("{\"requestId\":\"request\"}"))
				.andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"));
		verifyNoInteractions(service);
	}
	@Test void offIsSafeUnavailable() throws Exception {
		when(provider.getIfAvailable()).thenReturn(null);
		mvc.perform(post("/api/v1/auth/firebase/providers/relink/prepare").contentType("application/json")
				.content("{\"provider\":\"GOOGLE\",\"firebaseIdToken\":\"proof\"}"))
				.andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("PROVIDER_CHANGE_UNAVAILABLE"));
	}
	@Test void oversizedAndChunkedBodiesAreBounded() throws Exception {
		mvc.perform(post("/api/v1/auth/firebase/providers/unlink").header("Transfer-Encoding", "chunked")
				.contentType("application/json").content("x".repeat(24577)))
				.andExpect(status().isPayloadTooLarge()).andExpect(header().string("Cache-Control", "no-store"));
		verifyNoInteractions(service);
	}
	@Test void repeatedRequestsAreBoundedBeforeFirebaseVerification() throws Exception {
		for (int i = 0; i < 120; i++) mvc.perform(post("/api/v1/auth/firebase/providers/unlink/status").contentType("application/json").content("{}"));
		mvc.perform(post("/api/v1/auth/firebase/providers/unlink/status").contentType("application/json").content("{}"))
				.andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
		verifyNoInteractions(service);
	}
}
