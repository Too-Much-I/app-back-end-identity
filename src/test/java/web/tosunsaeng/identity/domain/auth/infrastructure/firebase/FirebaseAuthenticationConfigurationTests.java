package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseEnrollmentAttemptService;
import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.domain.repository.FirebaseEnrollmentAttemptRepository;

class FirebaseAuthenticationConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(FirebaseAuthenticationConfiguration.class);

	@Test
	void firebaseIsDisabledByDefaultWithoutInitializingSdkBeans() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(FirebaseAuthProperties.class);
			FirebaseAuthProperties properties = context.getBean(FirebaseAuthProperties.class);
			assertThat(properties.enabled()).isFalse();
			assertThat(properties.googleEnabled()).isFalse();
			assertThat(properties.appleEnabled()).isFalse();
			assertThat(properties.kakaoEnabled()).isFalse();
			assertThat(properties.phoneEnabled()).isFalse();
			assertThat(context).doesNotHaveBean(FirebaseAppHandle.class);
			assertThat(context).doesNotHaveBean(FirebaseAdminClient.class);
			assertThat(context).doesNotHaveBean(FirebaseAuthenticationVerifier.class);
			assertThat(context).doesNotHaveBean(FirebaseCredentialsProvider.class);
		});
	}

	@Test
	void enablingWithoutProjectIdFailsBeforeCredentialsAreLoaded() {
		contextRunner
				.withPropertyValues("app.firebase-auth.enabled=true")
				.run(context -> assertThat(context.getStartupFailure())
						.isNotNull()
						.hasStackTraceContaining(
								"app.firebase-auth.project-id is required"
						));
	}

	@Test
	void enabledConfigurationWiresInternalAdapterWithoutCallingExternalFirebase() {
		contextRunner
				.withPropertyValues(
						"app.firebase-auth.enabled=true",
						"app.firebase-auth.project-id=test-project"
				)
				.withBean(Clock.class, Clock::systemUTC)
				.withBean(
						FirebaseEnrollmentAttemptRepository.class,
						() -> mock(FirebaseEnrollmentAttemptRepository.class)
				)
				.withBean(
						"testFirebaseCredentialsProvider",
						FirebaseCredentialsProvider.class,
						this::fakeCredentialsProvider,
						definition -> definition.setPrimary(true)
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(FirebaseAppHandle.class);
					assertThat(context).hasSingleBean(FirebaseAdminClient.class);
					assertThat(context).hasSingleBean(FirebaseAuthenticationVerifier.class);
					assertThat(context).hasSingleBean(FirebaseEnrollmentAttemptService.class);
				});
	}

	private FirebaseCredentialsProvider fakeCredentialsProvider() {
		return () -> GoogleCredentials.create(new AccessToken(
				"test-only-firebase-access-token",
				Date.from(Instant.parse("2099-01-01T00:00:00Z"))
		));
	}
}
