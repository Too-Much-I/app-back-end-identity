package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.*;
import com.google.api.client.testing.http.MockLowLevelHttpRequest;
import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;

class FirebaseSessionRevocationHttpAdapterTests {
	static final Instant NOW = Instant.parse("2026-09-08T00:00:00Z");
	static final FirebaseSessionRevocationPort.Target TARGET = new FirebaseSessionRevocationPort.Target("test-project", null, "test-uid");
	@Test void mutation503IsNeverAutomaticallyRetried() {
		var transport = new Transport(503, 200);
		assertThatThrownBy(() -> adapter(transport, null).revoke(TARGET, NOW))
				.isInstanceOfSatisfying(FirebaseSessionRevocationPort.Failure.class,
						error -> assertThat(error.kind()).isEqualTo(FirebaseSessionRevocationPort.Failure.Kind.RESULT_UNKNOWN));
		assertThat(transport.calls).isEqualTo(1);
	}
	@Test void redirectIsNotFollowed() {
		var transport = new Transport(307, 200);
		assertThatThrownBy(() -> adapter(transport, null).revoke(TARGET, NOW)).isInstanceOf(FirebaseSessionRevocationPort.Failure.class);
		assertThat(transport.calls).isEqualTo(1);
	}
	@Test void mutationHasOnlyUidAndFixedDispatchBoundary() throws Exception {
		var transport = new Transport(200);
		adapter(transport, null).revoke(TARGET, NOW);
		assertThat(transport.url).isEqualTo("https://identitytoolkit.googleapis.com/v1/projects/test-project/accounts:update");
		var body = new ObjectMapper().readTree(transport.body);
		assertThat(body.size()).isEqualTo(2);
		assertThat(body.path("localId").asText()).isEqualTo("test-uid");
		assertThat(body.path("validSince").asLong()).isEqualTo(NOW.getEpochSecond());
	}
	@Test void projectOrTenantMismatchNeverSendsRequest() {
		var transport = new Transport(200);
		assertThatThrownBy(() -> adapter(transport, null).revoke(new FirebaseSessionRevocationPort.Target("wrong", null, "test-uid"), NOW))
				.isInstanceOf(FirebaseSessionRevocationPort.Failure.class);
		assertThatThrownBy(() -> adapter(transport, "tenant").revoke(TARGET, NOW)).isInstanceOf(FirebaseSessionRevocationPort.Failure.class);
		assertThat(transport.calls).isZero();
	}
	@Test void lookupValidatesExactUidAndParsesTimestampUnits() {
		var transport = new Transport(200);
		transport.responseBody = "{\"users\":[{\"localId\":\"test-uid\",\"createdAt\":\"1000\",\"validSince\":\"2000\",\"disabled\":false}]}";
		var snapshot = adapter(transport, null).inspect(TARGET);
		assertThat(snapshot.createdAt()).isEqualTo(Instant.ofEpochMilli(1000));
		assertThat(snapshot.validAfter()).isEqualTo(Instant.ofEpochSecond(2000));
		assertThat(transport.url).endsWith("/accounts:lookup");
	}
	@Test void lookupUnknownAccountDoesNotMeanSuccessfulRevoke() {
		var transport = new Transport(200); transport.responseBody = "{\"users\":[]}";
		assertThatThrownBy(() -> adapter(transport, null).inspect(TARGET)).isInstanceOfSatisfying(FirebaseSessionRevocationPort.Failure.class,
				error -> assertThat(error.kind()).isEqualTo(FirebaseSessionRevocationPort.Failure.Kind.TARGET_INVALID));
	}
	@Test void providerErrorContentIsNotExposed() {
		var transport = new Transport(400); transport.responseBody = "sensitive-provider-content";
		assertThatThrownBy(() -> adapter(transport, null).revoke(TARGET, NOW))
				.hasMessage("Firebase session operation failed.").hasNoCause();
	}
	@Test void tenantUsesExactScopedEndpoint() {
		var transport = new Transport(200);
		adapter(transport, "tenant-a").revoke(new FirebaseSessionRevocationPort.Target("test-project", "tenant-a", "test-uid"), NOW);
		assertThat(transport.url).isEqualTo("https://identitytoolkit.googleapis.com/v1/projects/test-project/tenants/tenant-a/accounts:update");
	}
	private FirebaseSessionRevocationHttpAdapter adapter(Transport transport, String tenant) {
		var properties = new FirebaseAuthProperties(true, "test-project", tenant, false, false, false, false,
				null, null, null, null, null, null, null, null);
		return new FirebaseSessionRevocationHttpAdapter(transport,
				GoogleCredentials.create(new AccessToken("fake-test-credential", new Date(Long.MAX_VALUE))), properties, new ObjectMapper());
	}
	static class Transport extends HttpTransport {
		final Deque<Integer> codes = new ArrayDeque<>(); int calls; String url; String body;
		String responseBody = "{\"localId\":\"test-uid\"}";
		Transport(Integer... codes) { this.codes.addAll(Arrays.asList(codes)); }
		@Override protected LowLevelHttpRequest buildRequest(String method, String url) {
			this.url = url;
			return new MockLowLevelHttpRequest(url) {
				@Override public LowLevelHttpResponse execute() throws IOException {
					calls++; body = getContentAsString();
					return new MockLowLevelHttpResponse().setStatusCode(codes.removeFirst())
							.addHeader("Location", "https://unexpected.example.invalid/")
							.setContentType("application/json").setContent(responseBody);
				}
			};
		}
	}
}
