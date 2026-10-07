package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ProviderChangeRequestFilterTests {
	@Test void recoveryIngressReturnsRemainingSecondsAndResetsAtNextMinute() throws Exception {
		Clock clock = mock(Clock.class);
		Instant now = Instant.parse("2026-10-07T00:00:40Z");
		when(clock.instant()).thenReturn(now);
		var filter = new ProviderChangeRequestFilter(clock);
		FilterChain chain = mock(FilterChain.class);
		for (int i = 0; i < 120; i++) filter.doFilter(request("/api/v1/auth/account-recovery/prepare"), new MockHttpServletResponse(), chain);
		var limited = new MockHttpServletResponse();
		filter.doFilter(request("/api/v1/auth/account-recovery/lookup"), limited, chain);
		assertThat(limited.getStatus()).isEqualTo(429);
		assertThat(limited.getHeader("Retry-After")).isEqualTo("20");
		assertThat(limited.getHeader("Cache-Control")).isEqualTo("no-store");
		assertThat(limited.getContentAsString()).contains("RECOVERY_RATE_LIMITED");
		when(clock.instant()).thenReturn(now.plusSeconds(19).plusNanos(999_999_999));
		var lastSecond = new MockHttpServletResponse();
		filter.doFilter(request("/api/v1/auth/account-recovery/prepare"), lastSecond, chain);
		assertThat(lastSecond.getHeader("Retry-After")).isEqualTo("1");
		when(clock.instant()).thenReturn(now.plusSeconds(20));
		filter.doFilter(request("/api/v1/auth/account-recovery/prepare"), new MockHttpServletResponse(), chain);
		verify(chain, times(121)).doFilter(any(), any());
	}
	@Test void providerEndpointsKeepTheirExistingDelay() throws Exception {
		var filter = new ProviderChangeRequestFilter(Clock.fixed(Instant.parse("2026-10-07T00:00:40Z"), ZoneOffset.UTC));
		FilterChain chain = mock(FilterChain.class);
		for (int i = 0; i < 120; i++) filter.doFilter(request("/api/v1/auth/firebase/providers/link/prepare"), new MockHttpServletResponse(), chain);
		var response = new MockHttpServletResponse();
		filter.doFilter(request("/api/v1/auth/firebase/providers/link/prepare"), response, chain);
		assertThat(response.getStatus()).isEqualTo(429);
		assertThat(response.getHeader("Retry-After")).isEqualTo("60");
	}
	private MockHttpServletRequest request(String path) {
		var request = new MockHttpServletRequest("POST", path);
		request.setRemoteAddr("127.0.0.1");
		return request;
	}
}
