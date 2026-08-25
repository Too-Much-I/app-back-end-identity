package web.tosunsaeng.identity.domain.user.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseCredentialsProvider;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseSdkWithdrawalCleanupAdapter;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseWithdrawalCleanupAppHandle;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.application.FirebaseWithdrawalCleanupPort;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalExternalCleanupWorker;
import web.tosunsaeng.identity.domain.user.application.WithdrawalCleanupRetryPolicy;
import web.tosunsaeng.identity.domain.user.application.WithdrawalCleanupTargetGuard;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UserWithdrawalExternalCleanupProperties.class)
public class UserWithdrawalExternalCleanupConfiguration {

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			prefix = "app.firebase-withdrawal-cleanup",
			name = "enabled",
			havingValue = "true"
	)
	static class EnabledCleanupConfiguration {

		@Bean(destroyMethod = "close")
		FirebaseWithdrawalCleanupAppHandle firebaseWithdrawalCleanupAppHandle(
				FirebaseAuthProperties firebaseAuthProperties,
				UserWithdrawalExternalCleanupProperties cleanupProperties,
				FirebaseCredentialsProvider credentialsProvider
		) {
			cleanupProperties.validate(firebaseAuthProperties);
			return FirebaseWithdrawalCleanupAppHandle.initialize(
					firebaseAuthProperties,
					cleanupProperties.getConnectTimeout(),
					cleanupProperties.getReadTimeout(),
					credentialsProvider
			);
		}

		@Bean
		FirebaseWithdrawalCleanupPort firebaseWithdrawalCleanupPort(
				FirebaseWithdrawalCleanupAppHandle cleanupAppHandle,
				FirebaseAuthProperties firebaseAuthProperties
		) {
			return new FirebaseSdkWithdrawalCleanupAdapter(
					cleanupAppHandle.firebaseApp(),
					firebaseAuthProperties
			);
		}

		@Bean
		WithdrawalCleanupTargetGuard withdrawalCleanupTargetGuard(
				UserWithdrawalLifecycleRepository lifecycleRepository,
				UserRepository userRepository,
				FirebaseIdentityRepository firebaseIdentityRepository
		) {
			return new WithdrawalCleanupTargetGuard(
					lifecycleRepository,
					userRepository,
					firebaseIdentityRepository
			);
		}

		@Bean
		WithdrawalCleanupRetryPolicy withdrawalCleanupRetryPolicy(
				UserWithdrawalExternalCleanupProperties properties
		) {
			return new WithdrawalCleanupRetryPolicy(
					properties.getInitialBackoff(),
					properties.getMaxBackoff(),
					() -> ThreadLocalRandom.current().nextDouble()
			);
		}

		@Bean
		UserWithdrawalExternalCleanupWorker userWithdrawalExternalCleanupWorker(
				UserWithdrawalLifecycleRepository lifecycleRepository,
				WithdrawalCleanupTargetGuard targetGuard,
				FirebaseWithdrawalCleanupPort cleanupPort,
				WithdrawalCleanupRetryPolicy retryPolicy,
				MeterRegistry meterRegistry,
				Clock clock,
				UserWithdrawalExternalCleanupProperties properties
		) {
			return new UserWithdrawalExternalCleanupWorker(
					lifecycleRepository,
					targetGuard,
					cleanupPort,
					retryPolicy,
					meterRegistry,
					clock,
					properties.getLeaseDuration(),
					properties.getMaxAttempts()
			);
		}

		@Bean
		UserWithdrawalExternalCleanupScheduler userWithdrawalExternalCleanupScheduler(
				UserWithdrawalExternalCleanupWorker worker,
				UserWithdrawalExternalCleanupProperties properties
		) {
			return new UserWithdrawalExternalCleanupScheduler(
					worker,
					properties.getMaxBatchSize()
			);
		}
	}
}
