package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventDeliveryException;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventDeliveryPort;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public final class LearningCoreOwnerEventDeliveryAdapter implements OwnerEventDeliveryPort {
	public static final String REQUIRED_PATH = "/internal/v1/owners/merge/events";
	private final URI endpoint;
	private final String audience;
	private final Duration readTimeout;
	private final WorkloadIdentityCredentialProvider credentialProvider;
	private final Clock clock;
	private final HttpClient httpClient;

	public LearningCoreOwnerEventDeliveryAdapter(
			URI endpoint, String audience, Duration connectTimeout, Duration readTimeout,
			WorkloadIdentityCredentialProvider credentialProvider, Clock clock
	) {
		if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())
				|| !REQUIRED_PATH.equals(endpoint.getPath()) || endpoint.getUserInfo() != null
				|| endpoint.getQuery() != null || endpoint.getFragment() != null) {
			throw new IllegalArgumentException("Learning Core endpoint is invalid");
		}
		this.endpoint = endpoint;
		this.audience = requireText(audience);
		this.readTimeout = Objects.requireNonNull(readTimeout);
		this.credentialProvider = Objects.requireNonNull(credentialProvider);
		this.clock = Objects.requireNonNull(clock);
		this.httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout)
				.followRedirects(HttpClient.Redirect.NEVER).build();
	}

	@Override
	public WorkloadDeliveryResult deliver(OwnerEventCore event, byte[] payload) {
		if (event.getEventType() != OwnerEventType.USER_MERGED) {
			throw new IllegalArgumentException("Phone rejoin event must not be sent to Learning Core");
		}
		WorkloadIdentityCredential credential;
		try {
			credential = Objects.requireNonNull(credentialProvider.issue(audience));
			if (!credential.expiresAt().isAfter(clock.instant())) throw new IllegalStateException();
		} catch (RuntimeException exception) {
			throw new OwnerEventDeliveryException(
					OwnerEventDeliveryException.Kind.CREDENTIAL_UNAVAILABLE, exception);
		}
		HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(readTimeout)
				.header("Authorization", "Bearer " + credential.tokenValue())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArray(payload)).build();
		try {
			HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
			return new WorkloadDeliveryResult(response.statusCode(), parseRetryAfter(
					response.headers().firstValue("Retry-After").orElse(null)));
		} catch (HttpTimeoutException exception) {
			throw new OwnerEventDeliveryException(OwnerEventDeliveryException.Kind.TIMEOUT, exception);
		} catch (ConnectException exception) {
			throw connection(exception);
		} catch (IOException exception) {
			throw connection(exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw connection(exception);
		}
	}

	private static Integer parseRetryAfter(String value) {
		if (value == null || !value.matches("[0-9]{1,3}")) return null;
		int parsed = Integer.parseInt(value);
		return parsed >= 1 && parsed <= 300 ? parsed : null;
	}

	private static String requireText(String value) {
		String required = Objects.requireNonNull(value).trim();
		if (required.isEmpty()) throw new IllegalArgumentException("audience must not be blank");
		return required;
	}

	private static OwnerEventDeliveryException connection(Exception exception) {
		return new OwnerEventDeliveryException(OwnerEventDeliveryException.Kind.CONNECTION, exception);
	}
}
