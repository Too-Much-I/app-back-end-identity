package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import java.io.IOException;
import java.util.Objects;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public final class FirebaseAppHandle implements AutoCloseable {

	static final String APP_NAME = "tosunsaeng-identity-firebase-auth";
	private static final Object INITIALIZATION_MONITOR = new Object();

	private final FirebaseApp firebaseApp;
	private final boolean owned;

	private FirebaseAppHandle(FirebaseApp firebaseApp, boolean owned) {
		this.firebaseApp = Objects.requireNonNull(firebaseApp, "firebaseApp must not be null");
		this.owned = owned;
	}

	public static FirebaseAppHandle initialize(
			FirebaseAuthProperties properties,
			FirebaseCredentialsProvider credentialsProvider
	) {
		Objects.requireNonNull(properties, "properties must not be null");
		Objects.requireNonNull(credentialsProvider, "credentialsProvider must not be null");
		if (!properties.enabled()) {
			throw new IllegalStateException("Firebase authentication is disabled.");
		}

		synchronized (INITIALIZATION_MONITOR) {
			FirebaseApp existing = findExistingApp();
			if (existing != null) {
				if (!Objects.equals(existing.getOptions().getProjectId(), properties.projectId())) {
					throw new IllegalStateException(
							"Existing Firebase application uses a different project configuration."
					);
				}
				return new FirebaseAppHandle(existing, false);
			}

			GoogleCredentials credentials;
			try {
				credentials = Objects.requireNonNull(
						credentialsProvider.load(),
						"Firebase credentials must not be null"
				);
			} catch (IOException | RuntimeException exception) {
				throw new IllegalStateException("Firebase credentials could not be loaded.");
			}

			FirebaseOptions options = FirebaseOptions.builder()
					.setCredentials(credentials)
					.setProjectId(properties.projectId())
					.setConnectTimeout(properties.connectTimeoutMillis())
					.setReadTimeout(properties.readTimeoutMillis())
					.setWriteTimeout(properties.readTimeoutMillis())
					.build();
			return new FirebaseAppHandle(FirebaseApp.initializeApp(options, APP_NAME), true);
		}
	}

	public FirebaseApp firebaseApp() {
		return firebaseApp;
	}

	@Override
	public void close() {
		if (!owned) {
			return;
		}
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
}
