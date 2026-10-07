package web.tosunsaeng.identity.domain.auth.accountrecovery;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueNoStoreFilter;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeRequestFilter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

class AccountRecoveryHttpTests {
	ObjectProvider<AccountRecoveryService> services = mock(ObjectProvider.class);
	AccountRecoveryService service = mock(AccountRecoveryService.class);
	MockMvc mvc;
	@BeforeEach void setup() {
		when(services.getIfAvailable()).thenReturn(service);
		mvc = MockMvcBuilders.standaloneSetup(new AccountRecoveryController(services))
				.setControllerAdvice(new GlobalExceptionHandler()).addFilters(new ReissueNoStoreFilter(), new ProviderChangeRequestFilter()).build();
	}
	@Test void noTokenRequiredAndFeatureOffFailsClosed() throws Exception {
		when(services.getIfAvailable()).thenReturn(null);
		mvc.perform(post("/api/v1/auth/account-recovery/prepare")).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("RECOVERY_UNAVAILABLE")).andExpect(header().string("Cache-Control", "no-store"));
		verifyNoInteractions(service);
	}
	@Test void lookupReturnsNoCredentialsAndRateLimitIncludesRetryAfter() throws Exception {
		when(service.lookup(anyString(), anyString(), anyString())).thenReturn(RecoveryResult.notFound());
		String body = "{\"recoveryId\":\"11111111-1111-4111-8111-111111111111\",\"firebaseIdToken\":\"private-proof\"}";
		String response = mvc.perform(post("/api/v1/auth/account-recovery/lookup").contentType("application/json").content(body))
				.andExpect(status().isOk()).andExpect(jsonPath("$.result.status").value("NOT_FOUND"))
				.andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsString();
		assertThat(response).doesNotContain("private-proof", "userId", "accessToken", "refreshToken", "phoneNumber");
		when(service.lookup(anyString(), anyString(), anyString())).thenThrow(new RecoveryRateLimitException(321));
		mvc.perform(post("/api/v1/auth/account-recovery/lookup").contentType("application/json").content(body))
				.andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "321"))
				.andExpect(jsonPath("$.code").value("RECOVERY_RATE_LIMITED"));
	}
	@Test void prepareReturnsRemainingDelayAndUpstreamThrottleDoesNotInventReset() throws Exception {
		when(service.prepare(anyString())).thenThrow(new RecoveryRateLimitException(20));
		mvc.perform(post("/api/v1/auth/account-recovery/prepare"))
				.andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "20"))
				.andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(jsonPath("$.code").value("RECOVERY_RATE_LIMITED"));
		when(service.lookup(anyString(), anyString(), anyString())).thenThrow(new AuthException(AuthErrorStatus.RECOVERY_RATE_LIMITED));
		mvc.perform(post("/api/v1/auth/account-recovery/lookup").contentType("application/json")
				.content("{\"recoveryId\":\"11111111-1111-4111-8111-111111111111\",\"firebaseIdToken\":\"test-proof\"}"))
				.andExpect(status().isTooManyRequests()).andExpect(header().doesNotExist("Retry-After"));
	}
	@Test void invalidOrOversizedInputNeverInvokesService() throws Exception {
		mvc.perform(post("/api/v1/auth/account-recovery/lookup").contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"));
		mvc.perform(post("/api/v1/auth/account-recovery/lookup").contentType("application/json").content("x".repeat(24577)))
				.andExpect(status().isPayloadTooLarge());
		verifyNoInteractions(service);
		assertThat(new AccountRecoveryController.LookupRequest("id", "private-proof").toString()).doesNotContain("private-proof");
	}
}
