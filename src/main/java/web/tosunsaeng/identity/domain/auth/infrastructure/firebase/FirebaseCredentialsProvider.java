package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;

@FunctionalInterface
public interface FirebaseCredentialsProvider {

	GoogleCredentials load() throws IOException;
}
