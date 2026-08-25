package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public final class FirebaseWithdrawalCleanupAppHandle implements AutoCloseable {

	static final String APP_NAME = "tosunsaeng-identity-firebase-withdrawal-cleanup";
	private static final Object INITIALIZATION_MONITOR = new Object();

	private final FirebaseApp firebaseApp;
	private final boolean owned;

	private FirebaseWithdrawalCleanupAppHandle(FirebaseApp firebaseApp, boolean owned) {
		this.firebaseApp = Objects.requireNonNull(firebaseApp, "firebaseApp must not be null");
		this.owned = owned;
	}

	public static FirebaseWithdrawalCleanupAppHandle initialize(
			FirebaseAuthProperties firebaseAuthProperties,
			Duration connectTimeout,
			Duration readTimeout,
			FirebaseCredentialsProvider credentialsProvider
	) {
		Objects.requireNonNull(firebaseAuthProperties, "firebaseAuthProperties must not be null");
		Objects.requireNonNull(credentialsProvider, "credentialsProvider must not be null");
		if (!firebaseAuthProperties.enabled() || firebaseAuthProperties.projectId() == null) {
			throw new IllegalStateException("Firebase authentication is disabled.");
		}
		int connectTimeoutMillis = positiveMillis(connectTimeout, "connectTimeout");
		int readTimeoutMillis = positiveMillis(readTimeout, "readTimeout");

		synchronized (INITIALIZATION_MONITOR) {
			FirebaseApp existing = findExistingApp();
			if (existing != null) {
				if (!Objects.equals(
						existing.getOptions().getProjectId(),
						firebaseAuthProperties.projectId()
				)) {
					throw new IllegalStateException(
							"Existing Firebase cleanup application uses a different project configuration."
					);
				}
				return new FirebaseWithdrawalCleanupAppHandle(existing, false);
			}

			GoogleCredentials credentials;
			try {
				credentials = Objects.requireNonNull(
						credentialsProvider.load(),
						"Firebase credentials must not be null"
				);
			} catch (IOException | RuntimeException exception) {
				throw new IllegalStateException("Firebase cleanup credentials could not be loaded.");
			}

			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(credentials)
					.setProjectId(firebaseAuthProperties.projectId())
					.setConnectTimeout(connectTimeoutMillis)
					.setReadTimeout(readTimeoutMillis)
					.setWriteTimeout(readTimeoutMillis)
					.build();
			return new FirebaseWithdrawalCleanupAppHandle(
					FirebaseApp.initializeApp(options, APP_NAME),
					true
			);
		}
	}

	public FirebaseApp firebaseApp() {
		return firebaseApp;
	}

	@Override
	public void close() {
		if (!owned) return;
		synchronized (INITIALIZATION_MONITOR) {
			if (FirebaseApp.getApps().contains(firebaseApp)) {
				firebaseApp.delete();
			}
		}
	}

	private static FirebaseApp findExistingApp() {
		return FirebaseApp.getApps().stream()
				.filter(app -> APP_NAME.equals(app.getName()))
				.findFirst()
				.orElse(null);
	}

	private static int positiveMillis(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		long milliseconds = required.toMillis();
		if (milliseconds <= 0 || milliseconds > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(fieldName + " must fit in positive milliseconds");
		}
		return Math.toIntExact(milliseconds);
	}
}
