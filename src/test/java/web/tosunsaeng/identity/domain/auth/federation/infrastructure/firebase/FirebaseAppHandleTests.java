package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FirebaseAppHandleTests {

	@BeforeEach
	@AfterEach
	void deleteTestApp() {
		FirebaseApp.getApps().stream()
				.filter(app -> FirebaseAppHandle.APP_NAME.equals(app.getName()))
				.forEach(FirebaseApp::delete);
	}

	@Test
	void repeatedInitializationReusesSameNamedApplicationWithoutReloadingCredentials() {
		AtomicInteger credentialLoads = new AtomicInteger();
		FirebaseCredentialsProvider credentialsProvider = () -> {
			credentialLoads.incrementAndGet();
			return fakeCredentials();
		};

		FirebaseAppHandle owner = FirebaseAppHandle.initialize(
				enabledProperties("test-project"),
				credentialsProvider
		);
		FirebaseAppHandle reused = FirebaseAppHandle.initialize(
				enabledProperties("test-project"),
				credentialsProvider
		);

		assertThat(reused.firebaseApp()).isSameAs(owner.firebaseApp());
		assertThat(credentialLoads).hasValue(1);
		reused.close();
		assertThat(FirebaseApp.getApps()).contains(owner.firebaseApp());
		owner.close();
		assertThat(FirebaseApp.getApps()).doesNotContain(owner.firebaseApp());
	}

	@Test
	void existingApplicationWithDifferentProjectFailsWithFixedSafeMessage() {
		FirebaseAppHandle owner = FirebaseAppHandle.initialize(
				enabledProperties("first-test-project"),
				this::fakeCredentials
		);

		assertThatThrownBy(() -> FirebaseAppHandle.initialize(
				enabledProperties("second-test-project"),
				this::fakeCredentials
		))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Existing Firebase application uses a different project configuration.")
				.hasMessageNotContaining("first-test-project")
				.hasMessageNotContaining("second-test-project");

		owner.close();
	}

	private GoogleCredentials fakeCredentials() {
		return GoogleCredentials.create(new AccessToken(
				"test-only-firebase-access-token",
				Date.from(Instant.parse("2099-01-01T00:00:00Z"))
		));
	}

	private FirebaseAuthProperties enabledProperties(String projectId) {
		return new FirebaseAuthProperties(
				true,
				projectId,
				null,
				true,
				true,
				false,
				true,
				"oidc.kakao",
				Duration.ofMinutes(15),
				Duration.ofMinutes(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}
}
