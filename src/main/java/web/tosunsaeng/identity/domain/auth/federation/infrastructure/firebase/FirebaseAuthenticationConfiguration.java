package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseEnrollmentAttemptService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupTransactionService;
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

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseAuthProperties.class)
public class FirebaseAuthenticationConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(
			prefix = "app.firebase-auth",
			name = "enabled",
			havingValue = "false",
			matchIfMissing = true
	)
	static class DisabledFirebaseAuthenticationConfiguration {

		@Bean
		FirebaseExchangeUseCase firebaseExchangeUseCase() {
			return new DisabledFirebaseExchangeUseCase();
		}

		@Bean
		FirebaseSignupUseCase firebaseSignupUseCase() {
			return new DisabledFirebaseSignupUseCase();
		}
	}

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

		@Bean
		FirebaseExchangeUseCase firebaseExchangeUseCase(
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				UserRepository userRepository,
				AccessTokenIssuer accessTokenIssuer,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseEnrollmentAttemptService enrollmentAttemptService,
				Clock clock
		) {
			return new FirebaseExchangeService(
					authenticationVerifier,
					firebaseIdentityRepository,
					socialIdentityRepository,
					userRepository,
					accessTokenIssuer,
					refreshSessionIssuer,
					enrollmentAttemptService,
					clock
			);
		}

		@Bean
		FirebaseSignupTransactionService firebaseSignupTransactionService(
				UserRepository userRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				PhoneIdentityRepository phoneIdentityRepository,
				PhoneFingerprintAliasRepository aliasRepository,
				PhoneEligibilityBindingOutboxRepository outboxRepository,
				PhoneEligibilityBindingRevisionRepository revisionRepository,
				SocialIdentityRepository socialIdentityRepository,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseEnrollmentAttemptRepository enrollmentRepository
		) {
			return new FirebaseSignupTransactionService(
					userRepository,
					firebaseIdentityRepository,
					phoneIdentityRepository,
					aliasRepository,
					outboxRepository,
					revisionRepository,
					socialIdentityRepository,
					refreshSessionIssuer,
					enrollmentRepository
			);
		}

		@Bean
		FirebaseSignupUseCase firebaseSignupUseCase(
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseEnrollmentAttemptRepository enrollmentRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				PhoneFingerprintAliasRepository aliasRepository,
				PhoneNumberNormalizer phoneNumberNormalizer,
				PhoneFingerprintHasher phoneFingerprintHasher,
				PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
				ConsentPolicy consentPolicy,
				UserFactory userFactory,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseSignupTransactionService transactionService,
				AccessTokenIssuer accessTokenIssuer,
				Clock clock
		) {
			return new FirebaseSignupService(
					authenticationVerifier,
					enrollmentRepository,
					firebaseIdentityRepository,
					socialIdentityRepository,
					aliasRepository,
					phoneNumberNormalizer,
					phoneFingerprintHasher,
					eligibilityFingerprintHasher,
					consentPolicy,
					userFactory,
					refreshSessionIssuer,
					transactionService,
					accessTokenIssuer,
					clock
			);
		}
	}
}
