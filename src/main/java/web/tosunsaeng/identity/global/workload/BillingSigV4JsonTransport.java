package web.tosunsaeng.identity.global.workload;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;

public final class BillingSigV4JsonTransport {

	public enum FailureKind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }

	public static final String SIGNING_NAME = "vpc-lattice-svcs";

	private final URI baseUri;
	private final String region;
	private final Duration readTimeout;
	private final AwsCredentialsProvider credentialsProvider;
	private final AwsV4HttpSigner signer;
	private final HttpClient httpClient;

	public BillingSigV4JsonTransport(
			URI baseUri,
			String region,
			Duration connectTimeout,
			Duration readTimeout,
			AwsCredentialsProvider credentialsProvider
	) {
		this(
				baseUri,
				region,
				readTimeout,
				credentialsProvider,
				AwsV4HttpSigner.create(),
				HttpClient.newBuilder()
						.connectTimeout(requirePositive(connectTimeout, "connectTimeout"))
						.followRedirects(HttpClient.Redirect.NEVER)
						.build()
		);
	}

	BillingSigV4JsonTransport(
			URI baseUri,
			String region,
			Duration readTimeout,
			AwsCredentialsProvider credentialsProvider,
			AwsV4HttpSigner signer,
			HttpClient httpClient
	) {
		this.baseUri = requireHttpsOrigin(baseUri);
		this.region = requireText(region, "region");
		this.readTimeout = requirePositive(readTimeout, "readTimeout");
		this.credentialsProvider = Objects.requireNonNull(credentialsProvider);
		this.signer = Objects.requireNonNull(signer);
		this.httpClient = Objects.requireNonNull(httpClient);
	}

	public WorkloadDeliveryResult post(String route, byte[] body) {
		URI endpoint = endpoint(route);
		byte[] requiredBody = Objects.requireNonNull(body, "body must not be null").clone();
		SdkHttpRequest unsigned = SdkHttpRequest.builder()
				.uri(endpoint)
				.method(SdkHttpMethod.POST)
				.putHeader("Content-Type", "application/json")
				.putHeader("traceparent", newTraceparent())
				.build();
		SignedRequest signed;
		try {
			signed = signer.sign(request -> request
					.identity(credentialsProvider.resolveCredentials())
					.request(unsigned)
					.payload(ContentStreamProvider.fromByteArray(requiredBody))
					.putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, SIGNING_NAME)
					.putProperty(AwsV4HttpSigner.REGION_NAME, region));
		} catch (RuntimeException exception) {
			throw new TransportException(FailureKind.CREDENTIAL_UNAVAILABLE, exception);
		}

		HttpRequest.Builder request = HttpRequest.newBuilder(endpoint)
				.timeout(readTimeout)
				.POST(HttpRequest.BodyPublishers.ofByteArray(requiredBody));
		signed.request().headers().forEach((name, values) -> {
			if (!isRestrictedHeader(name)) {
				values.forEach(value -> request.header(name, value));
			}
		});
		try {
			HttpResponse<Void> response = httpClient.send(
					request.build(), HttpResponse.BodyHandlers.discarding());
			return new WorkloadDeliveryResult(response.statusCode(), retryAfter(response));
		} catch (HttpTimeoutException exception) {
			throw new TransportException(FailureKind.TIMEOUT, exception);
		} catch (ConnectException exception) {
			throw new TransportException(FailureKind.CONNECTION, exception);
		} catch (IOException exception) {
			throw new TransportException(FailureKind.CONNECTION, exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new TransportException(FailureKind.CONNECTION, exception);
		}
	}

	private URI endpoint(String route) {
		String requiredRoute = requireText(route, "route");
		if (!requiredRoute.startsWith("/") || requiredRoute.contains("?")
				|| requiredRoute.contains("#") || requiredRoute.contains("//")) {
			throw new IllegalArgumentException("route must be a fixed absolute path");
		}
		return URI.create(baseUri.toString() + requiredRoute);
	}

	static Integer parseRetryAfter(List<String> values) {
		if (values == null || values.size() != 1) return null;
		String value = values.getFirst();
		if (value == null || !value.matches("[0-9]{1,3}")) return null;
		try {
			int seconds = Integer.parseInt(value);
			return seconds >= 1 && seconds <= WorkloadDeliveryResult.MAX_RETRY_AFTER_SECONDS
					? seconds : null;
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private static Integer retryAfter(HttpResponse<?> response) {
		return parseRetryAfter(response.headers().allValues("Retry-After"));
	}

	private static boolean isRestrictedHeader(String name) {
		String normalized = name.toLowerCase(Locale.ROOT);
		return normalized.equals("host") || normalized.equals("content-length");
	}

	private static URI requireHttpsOrigin(URI value) {
		URI uri = Objects.requireNonNull(value, "baseUri must not be null");
		if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
				|| uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
				|| (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
			throw new IllegalArgumentException("baseUri must be an HTTPS origin");
		}
		String authority = uri.getRawAuthority();
		return URI.create("https://" + authority);
	}

	private static Duration requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
		return value;
	}

	private static String requireText(String value, String name) {
		String required = Objects.requireNonNull(value, name + " must not be null").trim();
		if (required.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
		return required;
	}

	private static String newTraceparent() {
		String traceId = UUID.randomUUID().toString().replace("-", "");
		String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		return "00-" + traceId + "-" + spanId + "-01";
	}

	public static final class TransportException extends RuntimeException {
		private final FailureKind kind;

		public TransportException(FailureKind kind, Throwable cause) {
			super(Objects.requireNonNull(cause));
			this.kind = Objects.requireNonNull(kind);
		}

		public FailureKind kind() { return kind; }
	}
}
