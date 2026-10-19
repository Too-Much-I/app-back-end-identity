package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.Date;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.session.application.FirebaseSessionRevocationPort;

class FirebaseProviderMutationHttpAdapterTests {
	static final FirebaseSessionRevocationPort.Target TARGET = new FirebaseSessionRevocationPort.Target("test-project", null, "test-uid");
	FirebaseProviderMutationHttpAdapter adapter(FirebaseSessionRevocationHttpAdapterTests.Transport transport) {
		return new FirebaseProviderMutationHttpAdapter(transport, GoogleCredentials.create(new AccessToken("fake-test-only", new Date(Long.MAX_VALUE))),
				new FirebaseAuthProperties(true, "test-project", null, true, true, true, true, null, null, null, null, null, null, null, null), new ObjectMapper());
	}
	@ParameterizedTest @EnumSource(SocialProvider.class) void deletesOnlyRequestedProvider(SocialProvider provider) throws Exception {
		var transport = new FirebaseSessionRevocationHttpAdapterTests.Transport(200); adapter(transport).unlink(TARGET, provider);
		var body = new ObjectMapper().readTree(transport.body);
		assertThat(body.size()).isEqualTo(2); assertThat(body.path("deleteProvider").size()).isEqualTo(1);
		assertThat(body.path("deleteProvider").get(0).asText()).isEqualTo(switch (provider) {
			case GOOGLE -> "google.com"; case APPLE -> "apple.com"; case KAKAO -> "oidc.kakao";
		});
		assertThat(transport.url).isEqualTo("https://identitytoolkit.googleapis.com/v1/projects/test-project/accounts:update");
	}
	@ParameterizedTest @ValueSource(ints = {400, 401, 403, 429, 500, 503, 307})
	void mutationFailureIsNeverRetriedOrRedirected(int status) {
		var transport = new FirebaseSessionRevocationHttpAdapterTests.Transport(status, 200);
		assertThatThrownBy(() -> adapter(transport).unlink(TARGET, SocialProvider.GOOGLE))
				.isInstanceOfSatisfying(FirebaseSessionRevocationPort.Failure.class, e -> assertThat(e.kind()).isEqualTo(FirebaseSessionRevocationPort.Failure.Kind.RESULT_UNKNOWN));
		assertThat(transport.calls).isEqualTo(1);
	}
	@Test void snapshotReadsRawProviderSubjectsWithoutExposingThemInString() {
		var transport = new FirebaseSessionRevocationHttpAdapterTests.Transport(200);
		transport.responseBody = "{\"users\":[{\"localId\":\"test-uid\",\"createdAt\":\"1000\",\"validSince\":\"2000\",\"providerUserInfo\":[{\"providerId\":\"oidc.kakao\",\"rawId\":\"test-subject\"}]}]}";
		var snapshot = adapter(transport).inspect(TARGET);
		assertThat(snapshot.providers()).containsEntry(SocialProvider.KAKAO, "test-subject");
		assertThat(snapshot.createdAt()).isEqualTo(Instant.ofEpochMilli(1000)); assertThat(snapshot.toString()).doesNotContain("test-subject");
	}
	@Test void malformedOrOversizedSuccessIsNotAcknowledged() {
		for (var body : new String[]{"null", "{}", "x".repeat(65537)}) {
			var transport = new FirebaseSessionRevocationHttpAdapterTests.Transport(200); transport.responseBody = body;
			assertThatThrownBy(() -> adapter(transport).unlink(TARGET, SocialProvider.GOOGLE)).isInstanceOf(FirebaseSessionRevocationPort.Failure.class);
		}
	}
	@Test void wrongProjectNeverDispatches() {
		var transport = new FirebaseSessionRevocationHttpAdapterTests.Transport(200);
		assertThatThrownBy(() -> adapter(transport).unlink(new FirebaseSessionRevocationPort.Target("other", null, "test-uid"), SocialProvider.GOOGLE)).isInstanceOf(FirebaseSessionRevocationPort.Failure.class);
		assertThat(transport.calls).isZero();
	}
}
