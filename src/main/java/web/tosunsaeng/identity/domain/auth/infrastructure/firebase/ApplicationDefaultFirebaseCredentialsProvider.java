package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;

public final class ApplicationDefaultFirebaseCredentialsProvider
		implements FirebaseCredentialsProvider {

	@Override
	public GoogleCredentials load() throws IOException {
		return GoogleCredentials.getApplicationDefault();
	}
}
