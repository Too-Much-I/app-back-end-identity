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
import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;

public final class JdkUserMergedDeliveryAdapter implements UserMergedDeliveryPort {
	public static final String REQUIRED_PATH = "/internal/v1/events/user-merged";

	private final URI endpoint;
	private final Duration readTimeout;
	private final WorkloadIdentityCredentialProvider credentialProvider;
	private final HttpClient httpClient;
	private final Clock clock;

	public JdkUserMergedDeliveryAdapter(
			URI endpoint,
			Duration connectTimeout,
			Duration readTimeout,
			WorkloadIdentityCredentialProvider credentialProvider,
			Clock clock
	) {
		this(endpoint, readTimeout, credentialProvider, clock,
				HttpClient.newBuilder()
						.connectTimeout(Objects.requireNonNull(connectTimeout))
						.followRedirects(HttpClient.Redirect.NEVER)
						.build());
	}

	JdkUserMergedDeliveryAdapter(
			URI endpoint,
			Duration readTimeout,
			WorkloadIdentityCredentialProvider credentialProvider,
			Clock clock,
			HttpClient httpClient
	) {
		if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme())
				|| endpoint.getHost() == null || endpoint.getUserInfo() != null
				|| endpoint.getQuery() != null || endpoint.getFragment() != null
				|| !REQUIRED_PATH.equals(endpoint.getPath())) {
			throw new IllegalArgumentException("UserMerged endpoint is invalid");
		}
		this.endpoint = endpoint;
		this.readTimeout = Objects.requireNonNull(readTimeout);
		this.credentialProvider = Objects.requireNonNull(credentialProvider);
		this.clock = Objects.requireNonNull(clock);
		this.httpClient = Objects.requireNonNull(httpClient);
	}

	@Override
	public int deliver(byte[] payload) {
		Objects.requireNonNull(payload);
		WorkloadIdentityCredential credential;
		try {
			credential = Objects.requireNonNull(
					credentialProvider.issue(WorkloadIdentityPurpose.USER_MERGED)
			);
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
