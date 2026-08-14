package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseEnrollmentAttemptService;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseAuthProperties.class)
public class FirebaseAuthenticationConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(
			prefix = "app.firebase-auth",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledFirebaseAuthenticationConfiguration {

		@Bean
		FirebaseCredentialsProvider firebaseCredentialsProvider() {
			return new ApplicationDefaultFirebaseCredentialsProvider();
		}

		@Bean(destroyMethod = "close")
		FirebaseAppHandle firebaseAppHandle(
				FirebaseAuthProperties properties,
				FirebaseCredentialsProvider credentialsProvider
		) {
			return FirebaseAppHandle.initialize(properties, credentialsProvider);
		}

		@Bean
		FirebaseAdminClient firebaseAdminClient(
				FirebaseAppHandle firebaseAppHandle,
				FirebaseAuthProperties properties
		) {
			return new FirebaseSdkAdminClient(firebaseAppHandle.firebaseApp(), properties);
		}

		@Bean
		FirebaseAuthenticationVerifier firebaseAuthenticationVerifier(
				FirebaseAdminClient firebaseAdminClient,
				FirebaseAuthProperties properties,
				Clock clock
		) {
			return new FirebaseAdminAuthenticationVerifier(
					firebaseAdminClient,
					properties,
					clock
			);
		}

		@Bean
		FirebaseEnrollmentAttemptService firebaseEnrollmentAttemptService(
				FirebaseEnrollmentAttemptRepository repository,
				FirebaseAuthProperties properties,
				Clock clock
		) {
			return new FirebaseEnrollmentAttemptService(
					repository,
					clock,
					properties.enrollmentTtl(),
					properties.enrollmentCleanupRetention()
			);
		}
	}
}
