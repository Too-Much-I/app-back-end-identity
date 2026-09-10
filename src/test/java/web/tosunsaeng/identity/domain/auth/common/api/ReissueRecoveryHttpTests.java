package web.tosunsaeng.identity.domain.auth.common.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.local.application.LoginService;
import web.tosunsaeng.identity.domain.auth.registration.application.*;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueNoStoreFilter;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

class ReissueRecoveryHttpTests {
	TokenReissueService service;
	MockMvc mvc;
	@BeforeEach void setup() {
		service = mock(TokenReissueService.class);
		var controller = new AuthController(mock(EmailAvailabilityService.class), mock(SignupService.class), mock(GuestAuthService.class),
				mock(LoginService.class), service, mock(LogoutService.class), mock(LogoutAllService.class));
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())
				.addFilters(new ReissueNoStoreFilter()).build();
	}
	@Test void preservesBodyAndAbsoluteExpiriesAndRawHeaderMultiplicity() throws Exception {
		Instant access = Instant.parse("2026-09-09T00:30:00Z"), refresh = Instant.parse("2026-09-23T00:00:00Z");
		when(service.reissue(any(), anyList())).thenReturn(new ReissueResult(new ReissueResponse("test-access", "test-refresh", "Bearer", 1800000, 1209600000), access, refresh, access));
		var result = mvc.perform(post("/api/v1/auth/reissue").header("Idempotency-Key", "test-first", "test-second")
				.contentType("application/json").content("{\"refreshToken\":\"test-input\"}"))
				.andExpect(status().isOk()).andExpect(header().string("Reissue-Access-Expires-At", access.toString()))
				.andExpect(header().string("Reissue-Refresh-Expires-At", refresh.toString())).andExpect(header().string("Cache-Control", "no-store"))
				.andExpect(header().string("Pragma", "no-cache")).andExpect(jsonPath("$.result.accessTokenExpiresIn").value(1800000))
				.andExpect(jsonPath("$.result.refreshToken").value("test-refresh")).andReturn();
		verify(service).reissue(any(), eq(List.of("test-first", "test-second")));
		assertThat(result.getResponse().getContentAsString()).doesNotContain("recoveryUntil", "sessionId", "encryptionKeyId");
	}
	@Test void malformedBodyAndMissingRequestIdErrorsAreNeverCached() throws Exception {
		mvc.perform(post("/api/v1/auth/reissue").contentType("application/json").content("{}"))
				.andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"));
		when(service.reissue(any(), eq(List.of()))).thenThrow(new AuthException(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID));
		mvc.perform(post("/api/v1/auth/reissue").contentType("application/json").content("{\"refreshToken\":\"test-input\"}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REISSUE_REQUEST_ID"))
				.andExpect(header().string("Cache-Control", "no-store"));
	}
	@Test void unavailableAndExpiredResponsesExposeNoCredentialOrExpiry() throws Exception {
		for (var status : List.of(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE, AuthErrorStatus.REISSUE_RECOVERY_EXPIRED)) {
			reset(service); when(service.reissue(any(), anyList())).thenThrow(new AuthException(status));
			var response = mvc.perform(post("/api/v1/auth/reissue").header("Idempotency-Key", "test-id")
					.contentType("application/json").content("{\"refreshToken\":\"test-input\"}"))
					.andExpect(status().is(status.getHttpStatus().value())).andExpect(header().doesNotExist("Reissue-Access-Expires-At"))
					.andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
			assertThat(response.getContentAsString()).doesNotContain("test-input", "test-id");
		}
	}
}
