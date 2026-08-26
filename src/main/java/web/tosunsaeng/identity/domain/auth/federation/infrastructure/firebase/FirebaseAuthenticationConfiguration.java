package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseAuthMethodsSyncUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseGuestPrepareUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseGuestMergeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseGuestUpgradeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.DisabledFirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthMethodsSyncService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthMethodsSyncTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthMethodsSyncUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseEnrollmentAttemptService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestPrepareService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestPrepareUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestMergeService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestMergeTargetResolver;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestMergeTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestMergeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestUpgradeService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestUpgradeTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseGuestUpgradeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseIdentityOwnershipService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.WithdrawalEnrollmentGate;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityTransactionService;
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
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
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

		@Bean
		FirebaseGuestPrepareUseCase firebaseGuestPrepareUseCase() {
			return new DisabledFirebaseGuestPrepareUseCase();
		}

		@Bean
		FirebaseGuestUpgradeUseCase firebaseGuestUpgradeUseCase() {
			return new DisabledFirebaseGuestUpgradeUseCase();
		}

		@Bean
		FirebaseGuestMergeUseCase firebaseGuestMergeUseCase() {
			return new DisabledFirebaseGuestMergeUseCase();
		}

		@Bean
		FirebaseAuthMethodsSyncUseCase firebaseAuthMethodsSyncUseCase() {
			return new DisabledFirebaseAuthMethodsSyncUseCase();
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
		WithdrawalEnrollmentGate withdrawalEnrollmentGate(
				UserRepository userRepository,
				UserWithdrawalLifecycleRepository lifecycleRepository
		) {
			return new WithdrawalEnrollmentGate(userRepository, lifecycleRepository);
		}

		@Bean
		FirebaseIdentityOwnershipService firebaseIdentityOwnershipService(
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				UserRepository userRepository,
				WithdrawalEnrollmentGate withdrawalEnrollmentGate
		) {
			return new FirebaseIdentityOwnershipService(
					firebaseIdentityRepository,
					socialIdentityRepository,
					userRepository,
					withdrawalEnrollmentGate
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
				Clock clock,
				@Nullable UserWithdrawalLifecycleRepository withdrawalLifecycleRepository
		) {
			return new FirebaseExchangeService(
					authenticationVerifier,
					firebaseIdentityRepository,
					socialIdentityRepository,
					userRepository,
					accessTokenIssuer,
					refreshSessionIssuer,
					enrollmentAttemptService,
					clock,
					withdrawalLifecycleRepository
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
				Clock clock,
				WithdrawalEnrollmentGate withdrawalEnrollmentGate
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
					clock,
					withdrawalEnrollmentGate
			);
		}

		@Bean
		FirebaseGuestPrepareUseCase firebaseGuestPrepareUseCase(
				CurrentUserProvider currentUserProvider,
				UserRepository userRepository,
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseIdentityOwnershipService ownershipService,
				FirebaseEnrollmentAttemptService enrollmentAttemptService,
				Clock clock
		) {
			return new FirebaseGuestPrepareService(
					currentUserProvider,
					userRepository,
					authenticationVerifier,
					ownershipService,
					enrollmentAttemptService,
					clock
			);
		}

		@Bean
		FirebaseGuestUpgradeTransactionService firebaseGuestUpgradeTransactionService(
				UserRepository userRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				PhoneIdentityTransactionService phoneIdentityTransactionService,
				RefreshSessionRepository refreshSessionRepository,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseEnrollmentAttemptRepository enrollmentRepository
		) {
			return new FirebaseGuestUpgradeTransactionService(
					userRepository,
					firebaseIdentityRepository,
					socialIdentityRepository,
					phoneIdentityTransactionService,
					refreshSessionRepository,
					refreshSessionIssuer,
					enrollmentRepository
			);
		}

		@Bean
		FirebaseGuestUpgradeUseCase firebaseGuestUpgradeUseCase(
				CurrentUserProvider currentUserProvider,
				UserRepository userRepository,
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseEnrollmentAttemptRepository enrollmentRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				PhoneFingerprintAliasRepository aliasRepository,
				FirebaseIdentityOwnershipService ownershipService,
				PhoneNumberNormalizer phoneNumberNormalizer,
				PhoneFingerprintHasher phoneFingerprintHasher,
				PhoneEligibilityFingerprintHasher eligibilityFingerprintHasher,
				ConsentPolicy consentPolicy,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseGuestUpgradeTransactionService transactionService,
				AccessTokenIssuer accessTokenIssuer,
				Clock clock
		) {
			return new FirebaseGuestUpgradeService(
					currentUserProvider,
					userRepository,
					authenticationVerifier,
					enrollmentRepository,
					firebaseIdentityRepository,
					socialIdentityRepository,
					aliasRepository,
					ownershipService,
					phoneNumberNormalizer,
					phoneFingerprintHasher,
					eligibilityFingerprintHasher,
					consentPolicy,
					refreshSessionIssuer,
					transactionService,
					accessTokenIssuer,
					clock
			);
		}

		@Bean
		@ConditionalOnProperty(
				prefix = "app.guest-merge",
				name = "enabled",
				havingValue = "false",
				matchIfMissing = true
		)
		FirebaseGuestMergeUseCase disabledFirebaseGuestMergeUseCase() {
			return new DisabledFirebaseGuestMergeUseCase();
		}

		@Bean
		@ConditionalOnProperty(
				prefix = "app.guest-merge",
				name = "enabled",
				havingValue = "true"
		)
		FirebaseGuestMergeTargetResolver firebaseGuestMergeTargetResolver(
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				UserRepository userRepository,
				WithdrawalEnrollmentGate withdrawalEnrollmentGate
		) {
			return new FirebaseGuestMergeTargetResolver(
					firebaseIdentityRepository,
					socialIdentityRepository,
					userRepository,
					withdrawalEnrollmentGate
			);
		}

		@Bean
		@ConditionalOnProperty(
				prefix = "app.guest-merge",
				name = "enabled",
				havingValue = "true"
		)
		FirebaseGuestMergeTransactionService firebaseGuestMergeTransactionService(
				UserRepository userRepository,
				RefreshSessionRepository refreshSessionRepository,
				RefreshSessionIssuer refreshSessionIssuer,
				UserMergedOutboxRepository outboxRepository
		) {
			return new FirebaseGuestMergeTransactionService(
					userRepository,
					refreshSessionRepository,
					refreshSessionIssuer,
					outboxRepository
			);
		}

		@Bean
		@ConditionalOnProperty(
				prefix = "app.guest-merge",
				name = "enabled",
				havingValue = "true"
		)
		FirebaseGuestMergeUseCase enabledFirebaseGuestMergeUseCase(
				CurrentUserProvider currentUserProvider,
				UserRepository userRepository,
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseGuestMergeTargetResolver targetResolver,
				RefreshSessionIssuer refreshSessionIssuer,
				FirebaseGuestMergeTransactionService transactionService,
				AccessTokenIssuer accessTokenIssuer,
				Clock clock
		) {
			return new FirebaseGuestMergeService(
					currentUserProvider,
					userRepository,
					authenticationVerifier,
					targetResolver,
					refreshSessionIssuer,
					transactionService,
					accessTokenIssuer,
					clock
			);
		}

		@Bean
		FirebaseAuthMethodsSyncTransactionService firebaseAuthMethodsSyncTransactionService(
				SocialIdentityRepository socialIdentityRepository
		) {
			return new FirebaseAuthMethodsSyncTransactionService(socialIdentityRepository);
		}

		@Bean
		FirebaseAuthMethodsSyncUseCase firebaseAuthMethodsSyncUseCase(
				CurrentUserProvider currentUserProvider,
				UserRepository userRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				SocialIdentityRepository socialIdentityRepository,
				FirebaseAuthenticationVerifier authenticationVerifier,
				FirebaseAuthMethodsSyncTransactionService transactionService,
				Clock clock,
				WithdrawalEnrollmentGate withdrawalEnrollmentGate
		) {
			return new FirebaseAuthMethodsSyncService(
					currentUserProvider,
					userRepository,
					firebaseIdentityRepository,
					socialIdentityRepository,
					authenticationVerifier,
					transactionService,
					clock,
					withdrawalEnrollmentGate
			);
		}
	}
}
