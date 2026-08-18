package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseEnrollmentAttemptService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;

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
			assertThat(context).hasSingleBean(FirebaseExchangeUseCase.class);
			assertThat(context).hasSingleBean(FirebaseSignupUseCase.class);
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
				.withBean(FirebaseIdentityRepository.class, () -> mock(FirebaseIdentityRepository.class))
				.withBean(SocialIdentityRepository.class, () -> mock(SocialIdentityRepository.class))
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(AccessTokenIssuer.class, () -> mock(AccessTokenIssuer.class))
				.withBean(RefreshSessionIssuer.class, () -> mock(RefreshSessionIssuer.class))
				.withBean(PhoneIdentityRepository.class, () -> mock(PhoneIdentityRepository.class))
				.withBean(PhoneFingerprintAliasRepository.class, () -> mock(PhoneFingerprintAliasRepository.class))
				.withBean(PhoneEligibilityBindingOutboxRepository.class, () -> mock(PhoneEligibilityBindingOutboxRepository.class))
				.withBean(PhoneEligibilityBindingRevisionRepository.class, () -> mock(PhoneEligibilityBindingRevisionRepository.class))
				.withBean(PhoneNumberNormalizer.class, () -> mock(PhoneNumberNormalizer.class))
				.withBean(PhoneFingerprintHasher.class, () -> mock(PhoneFingerprintHasher.class))
				.withBean(PhoneEligibilityFingerprintHasher.class, () -> mock(PhoneEligibilityFingerprintHasher.class))
				.withBean(ConsentPolicy.class, () -> mock(ConsentPolicy.class))
				.withBean(UserFactory.class, () -> mock(UserFactory.class))
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
					assertThat(context).hasSingleBean(FirebaseExchangeUseCase.class);
					assertThat(context).hasSingleBean(FirebaseSignupUseCase.class);
				});
	}

	private FirebaseCredentialsProvider fakeCredentialsProvider() {
		return () -> GoogleCredentials.create(new AccessToken(
				"test-only-firebase-access-token",
				Date.from(Instant.parse("2099-01-01T00:00:00Z"))
		));
	}
}
