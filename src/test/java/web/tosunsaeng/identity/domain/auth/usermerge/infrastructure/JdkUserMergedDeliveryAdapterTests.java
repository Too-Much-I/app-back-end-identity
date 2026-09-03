package web.tosunsaeng.identity.domain.auth.usermerge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;

class JdkUserMergedDeliveryAdapterTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

	@Test
	@SuppressWarnings("unchecked")
	void legacyDeliveryUsesExactEndpointAndUserMergedPurpose() throws Exception {
		HttpClient client = mock(HttpClient.class);
		HttpResponse<Void> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(204);
		when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
				.thenReturn(response);
		AtomicReference<WorkloadIdentityPurpose> purpose = new AtomicReference<>();
		JdkUserMergedDeliveryAdapter adapter = new JdkUserMergedDeliveryAdapter(
				URI.create("https://learning.internal/internal/v1/events/user-merged"),
				Duration.ofSeconds(3), requestedPurpose -> {
					purpose.set(requestedPurpose);
					return new WorkloadIdentityCredential(
							"workload-token", NOW, NOW.plusSeconds(120));
				}, Clock.fixed(NOW, ZoneOffset.UTC), client);

		assertThat(adapter.deliver("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
				.isEqualTo(204);
		assertThat(purpose.get()).isEqualTo(WorkloadIdentityPurpose.USER_MERGED);
	}
}
