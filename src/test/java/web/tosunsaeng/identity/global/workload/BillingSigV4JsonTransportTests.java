package web.tosunsaeng.identity.global.workload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.Authenticator;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;

class BillingSigV4JsonTransportTests {
	@Test
	void signsExactPostPathBodyRegionAndLatticeService() {
		CapturingHttpClient client = new CapturingHttpClient(204, Map.of("Retry-After", List.of("30")));
		BillingSigV4JsonTransport transport = new BillingSigV4JsonTransport(
				URI.create("https://billing.internal"), "ap-northeast-2", Duration.ofSeconds(3),
				StaticCredentialsProvider.create(AwsBasicCredentials.create("test-key", "test-secret")),
				AwsV4HttpSigner.create(), client);
		byte[] payload = "{\"eventId\":\"exact\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		WorkloadDeliveryResult result = transport.post("/internal/v1/eligibility/trial/events", payload);

		HttpRequest request = client.request.get();
		assertThat(request.method()).isEqualTo("POST");
		assertThat(request.uri()).isEqualTo(URI.create(
				"https://billing.internal/internal/v1/eligibility/trial/events"));
		assertThat(request.headers().firstValue("Authorization").orElseThrow())
				.contains("Credential=test-key/", "/ap-northeast-2/vpc-lattice-svcs/aws4_request",
						"traceparent");
		assertThat(request.headers().firstValue("traceparent").orElseThrow())
				.matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01");
		assertThat(readBody(request)).isEqualTo(payload);
		assertThat(result).isEqualTo(new WorkloadDeliveryResult(204, 30));
	}

	@Test
	void acceptsOnlyOneIntegerRetryAfterFromOneToThreeHundred() {
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("1"))).isEqualTo(1);
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("300"))).isEqualTo(300);
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("0"))).isNull();
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("301"))).isNull();
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("Wed, 21 Oct 2015 07:28:00 GMT"))).isNull();
		assertThat(BillingSigV4JsonTransport.parseRetryAfter(List.of("1", "2"))).isNull();
	}

	@Test
	void rejectsNonOriginOrNonHttpsBaseUrl() {
		assertThatThrownBy(() -> new BillingSigV4JsonTransport(
				URI.create("http://billing.internal/path"), "ap-northeast-2",
				Duration.ofSeconds(1), Duration.ofSeconds(3),
				StaticCredentialsProvider.create(AwsBasicCredentials.create("a", "b"))))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static byte[] readBody(HttpRequest request) {
		java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
		request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
			@Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
			@Override public void onNext(java.nio.ByteBuffer item) {
				byte[] bytes = new byte[item.remaining()]; item.get(bytes); output.writeBytes(bytes);
			}
			@Override public void onError(Throwable throwable) { throw new AssertionError(throwable); }
			@Override public void onComplete() {}
		});
		return output.toByteArray();
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

		@Override public <T> HttpResponse<T> send(HttpRequest request,
				HttpResponse.BodyHandler<T> responseBodyHandler) {
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
