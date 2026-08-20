package web.tosunsaeng.identity.domain.auth.usermerge.infrastructure;

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

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedDeliveryException;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedDeliveryPort;

public final class JdkUserMergedDeliveryAdapter implements UserMergedDeliveryPort {

	private final URI endpoint;
	private final String audience;
	private final Duration readTimeout;
	private final WorkloadIdentityCredentialProvider credentialProvider;
	private final HttpClient httpClient;
	private final Clock clock;

	public JdkUserMergedDeliveryAdapter(
			URI endpoint,
			String audience,
			Duration connectTimeout,
			Duration readTimeout,
			WorkloadIdentityCredentialProvider credentialProvider,
			Clock clock
	) {
		if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())) {
			throw new IllegalArgumentException("delivery endpoint must use HTTPS");
		}
		this.endpoint = endpoint;
		this.audience = Objects.requireNonNull(audience);
		this.readTimeout = Objects.requireNonNull(readTimeout);
		this.credentialProvider = Objects.requireNonNull(credentialProvider);
		this.clock = Objects.requireNonNull(clock);
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Objects.requireNonNull(connectTimeout))
				.followRedirects(HttpClient.Redirect.NEVER)
				.build();
	}

	@Override
	public int deliver(byte[] payload) {
		Objects.requireNonNull(payload);
		WorkloadIdentityCredential credential;
		try {
			credential = Objects.requireNonNull(credentialProvider.issue(audience));
			if (!credential.expiresAt().isAfter(clock.instant())) {
				throw new IllegalStateException();
			}
		} catch (RuntimeException exception) {
			throw new UserMergedDeliveryException(
					UserMergedDeliveryException.Kind.CREDENTIAL_UNAVAILABLE,
					exception
			);
		}
		HttpRequest request = HttpRequest.newBuilder(endpoint)
				.timeout(readTimeout)
				.header("Authorization", "Bearer " + credential.tokenValue())
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArray(payload))
				.build();
		try {
			return httpClient.send(
					request,
					HttpResponse.BodyHandlers.discarding()
			).statusCode();
		} catch (HttpTimeoutException exception) {
			throw new UserMergedDeliveryException(
					UserMergedDeliveryException.Kind.TIMEOUT,
					exception
			);
		} catch (ConnectException exception) {
			throw connectionFailure(exception);
		} catch (IOException exception) {
			throw connectionFailure(exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw connectionFailure(exception);
		}
	}

	private UserMergedDeliveryException connectionFailure(Exception exception) {
		return new UserMergedDeliveryException(
				UserMergedDeliveryException.Kind.CONNECTION,
				exception
		);
	}
}
