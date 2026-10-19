package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;
import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;

class LearningCoreOwnerEventDeliveryAdapterTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
	private static final URI ENDPOINT = URI.create(
			"https://learning-core.internal/internal/v1/events/user-merged");

	@Test
	void sendsExactJsonPostWithUserMergedWorkloadPurpose() {
		CapturingHttpClient client = new CapturingHttpClient(
				429, Map.of("Retry-After", List.of("30")));
		AtomicReference<WorkloadIdentityPurpose> purpose = new AtomicReference<>();
		LearningCoreOwnerEventDeliveryAdapter adapter =
				new LearningCoreOwnerEventDeliveryAdapter(
						ENDPOINT, Duration.ofSeconds(3), requestedPurpose -> {
							purpose.set(requestedPurpose);
							return new WorkloadIdentityCredential(
									"workload-token", NOW, NOW.plusSeconds(120));
						}, Clock.fixed(NOW, ZoneOffset.UTC), client);
		byte[] payload = "{\"eventId\":\"exact\"}"
				.getBytes(java.nio.charset.StandardCharsets.UTF_8);

		WorkloadDeliveryResult result = adapter.deliver(
				OwnerEventCore.userMerged(
						"00000000-0000-4000-8000-000000000001",
						"00000000-0000-4000-8000-000000000002", NOW),
				payload);

		HttpRequest request = client.request.get();
		assertThat(purpose.get()).isEqualTo(WorkloadIdentityPurpose.USER_MERGED);
		assertThat(request.method()).isEqualTo("POST");
		assertThat(request.uri()).isEqualTo(ENDPOINT);
		assertThat(request.headers().firstValue("Authorization"))
				.contains("Bearer workload-token");
		assertThat(request.headers().firstValue("Content-Type"))
				.contains("application/json");
		assertThat(result).isEqualTo(new WorkloadDeliveryResult(429, 30));
	}

	@Test
	void requiresExactHttpsEndpointAndNeverFollowsRedirects() {
		assertThat(LearningCoreOwnerEventDeliveryAdapter.createHttpClient(
				Duration.ofSeconds(1)).followRedirects()).isEqualTo(HttpClient.Redirect.NEVER);
		for (String invalid : List.of(
				"http://learning-core.internal/internal/v1/events/user-merged",
				"https://learning-core.internal/internal/v1/owners/merge/events",
				"https://learning-core.internal/internal/v1/events/user-merged?query=true",
				"https://learning-core.internal/internal/v1/events/user-merged#fragment",
				"https://user@learning-core.internal/internal/v1/events/user-merged")) {
			assertThatThrownBy(() -> new LearningCoreOwnerEventDeliveryAdapter(
					URI.create(invalid), Duration.ofSeconds(3), purpose -> credential(),
					Clock.fixed(NOW, ZoneOffset.UTC), new CapturingHttpClient(204, Map.of())))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Test
	void acceptsOnlyBoundedDeltaSecondsRetryAfter() {
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(List.of("1")))
				.isEqualTo(1);
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(List.of("300")))
				.isEqualTo(300);
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(List.of("0"))).isNull();
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(List.of("301"))).isNull();
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(
				List.of("Wed, 21 Oct 2015 07:28:00 GMT"))).isNull();
		assertThat(LearningCoreOwnerEventDeliveryAdapter.parseRetryAfter(
				List.of("30", "60"))).isNull();
	}

	private static WorkloadIdentityCredential credential() {
		return new WorkloadIdentityCredential("workload-token", NOW, NOW.plusSeconds(120));
	}

	private static final class CapturingHttpClient extends HttpClient {
		private final AtomicReference<HttpRequest> request = new AtomicReference<>();
		private final int status;
		private final HttpHeaders headers;

		private CapturingHttpClient(int status, Map<String, List<String>> headers) {
			this.status = status;
			this.headers = HttpHeaders.of(headers, (name, value) -> true);
		}

		@Override public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
		@Override public Optional<Duration> connectTimeout() { return Optional.of(Duration.ofSeconds(1)); }
		@Override public Redirect followRedirects() { return Redirect.NEVER; }
		@Override public Optional<ProxySelector> proxy() { return Optional.empty(); }
		@Override public SSLContext sslContext() { return null; }
		@Override public SSLParameters sslParameters() { return new SSLParameters(); }
		@Override public Optional<Authenticator> authenticator() { return Optional.empty(); }
		@Override public Version version() { return Version.HTTP_1_1; }
		@Override public Optional<Executor> executor() { return Optional.empty(); }

		@Override public <T> HttpResponse<T> send(
				HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
			this.request.set(request);
			HttpResponse<T> response = mock(HttpResponse.class);
			when(response.statusCode()).thenReturn(status);
			when(response.headers()).thenReturn(headers);
			return response;
		}

		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
				HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
			throw new UnsupportedOperationException();
		}

		@Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
				HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler,
				HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
			throw new UnsupportedOperationException();
		}
	}
}
