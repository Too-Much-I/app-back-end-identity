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

class FirebaseWithdrawalCleanupAppHandleTests {

	@BeforeEach
	@AfterEach
	void deleteTestApp() {
		FirebaseApp.getApps().stream()
				.filter(app -> FirebaseWithdrawalCleanupAppHandle.APP_NAME.equals(app.getName()))
				.forEach(FirebaseApp::delete);
	}

	@Test
	void initializesDedicatedAppWithIndependentTimeoutsAndReusesIt() {
		AtomicInteger loads = new AtomicInteger();
		FirebaseCredentialsProvider credentialsProvider = () -> {
			loads.incrementAndGet();
			return fakeCredentials();
		};
		FirebaseWithdrawalCleanupAppHandle owner = FirebaseWithdrawalCleanupAppHandle.initialize(
				enabledProperties("test-project"),
				Duration.ofSeconds(2),
				Duration.ofSeconds(4),
				credentialsProvider
		);
		FirebaseWithdrawalCleanupAppHandle reused = FirebaseWithdrawalCleanupAppHandle.initialize(
				enabledProperties("test-project"),
				Duration.ofSeconds(2),
				Duration.ofSeconds(4),
				credentialsProvider
		);

		assertThat(owner.firebaseApp().getName())
				.isEqualTo(FirebaseWithdrawalCleanupAppHandle.APP_NAME);
		assertThat(owner.firebaseApp().getOptions().getConnectTimeout()).isEqualTo(2_000);
		assertThat(owner.firebaseApp().getOptions().getReadTimeout()).isEqualTo(4_000);
		assertThat(reused.firebaseApp()).isSameAs(owner.firebaseApp());
		assertThat(loads).hasValue(1);
		reused.close();
		assertThat(FirebaseApp.getApps()).contains(owner.firebaseApp());
		owner.close();
		assertThat(FirebaseApp.getApps()).doesNotContain(owner.firebaseApp());
	}

	@Test
	void rejectsDisabledFirebaseAndInvalidTimeoutWithSafeMessages() {
		assertThatThrownBy(() -> FirebaseWithdrawalCleanupAppHandle.initialize(
				disabledProperties(),
				Duration.ofSeconds(1),
				Duration.ofSeconds(1),
				this::fakeCredentials
		)).isInstanceOf(IllegalStateException.class)
				.hasMessage("Firebase authentication is disabled.");
		assertThatThrownBy(() -> FirebaseWithdrawalCleanupAppHandle.initialize(
				enabledProperties("test-project"),
				Duration.ZERO,
				Duration.ofSeconds(1),
				this::fakeCredentials
		)).isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining("test-project");
	}

	private GoogleCredentials fakeCredentials() {
		return GoogleCredentials.create(new AccessToken(
				"test-only-cleanup-access-token",
				Date.from(Instant.parse("2099-01-01T00:00:00Z"))
		));
	}

	private FirebaseAuthProperties enabledProperties(String projectId) {
		return properties(true, projectId);
	}

	private FirebaseAuthProperties disabledProperties() {
		return properties(false, null);
	}

	private FirebaseAuthProperties properties(boolean enabled, String projectId) {
		return new FirebaseAuthProperties(
				enabled,
				projectId,
				null,
				true,
				false,
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
