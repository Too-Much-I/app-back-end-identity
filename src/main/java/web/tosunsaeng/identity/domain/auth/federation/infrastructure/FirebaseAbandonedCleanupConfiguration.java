package web.tosunsaeng.identity.domain.auth.federation.infrastructure;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseCleanupRetryPolicy;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseCleanupTerminalTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseEnrollmentCleanupWorker;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseEnrollmentTargetGuard;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseUserCleanupPort;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAbandonedEnrollmentCaptureTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FirebaseAbandonedCleanupProperties.class)
public class FirebaseAbandonedCleanupConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(
			prefix = "app.firebase-abandoned-cleanup",
			name = "capture-enabled",
			havingValue = "true"
	)
	static class EnabledCaptureConfiguration {

		@Bean
		FirebaseAbandonedEnrollmentCaptureTransactionService firebaseAbandonedEnrollmentCaptureTransactionService(
				FirebaseEnrollmentAttemptRepository attemptRepository,
				AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
				FirebaseIdentityRepository firebaseIdentityRepository,
				UserRepository userRepository
		) {
			return new FirebaseAbandonedEnrollmentCaptureTransactionService(
					attemptRepository, cleanupRepository, firebaseIdentityRepository, userRepository
			);
		}

		@Bean
		FirebaseAbandonedEnrollmentCaptureRunner firebaseAbandonedEnrollmentCaptureRunner(
				FirebaseEnrollmentAttemptRepository attemptRepository,
				FirebaseAbandonedEnrollmentCaptureTransactionService transactionService,
				AbandonedFirebaseUserCleanupPort cleanupPort,
				AbandonedFirebaseEnrollmentTargetGuard targetGuard,
				FirebaseAbandonedCleanupProperties properties,
				Clock clock
		) {
			return new FirebaseAbandonedEnrollmentCaptureRunner(
					attemptRepository, transactionService, cleanupPort, targetGuard,
					properties, clock
			);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(
			prefix = "app.firebase-abandoned-cleanup",
			name = "worker-enabled",
			havingValue = "true"
	)
	static class EnabledWorkerConfiguration {

		@Bean
		AbandonedFirebaseCleanupRetryPolicy abandonedFirebaseCleanupRetryPolicy(
				FirebaseAbandonedCleanupProperties properties
		) {
			properties.validate();
			return new AbandonedFirebaseCleanupRetryPolicy(
					properties.getInitialBackoff(), properties.getMaxBackoff(),
					() -> ThreadLocalRandom.current().nextDouble()
			);
		}

		@Bean
		AbandonedFirebaseCleanupTerminalTransactionService cleanupTerminalTransactionService(
				AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
				FirebaseEnrollmentAttemptRepository attemptRepository,
				FirebaseAbandonedCleanupProperties properties
		) {
			return new AbandonedFirebaseCleanupTerminalTransactionService(
					cleanupRepository, attemptRepository,
					properties.getTerminalLifecycleRetention(),
					properties.getTerminalEnrollmentRetention()
			);
		}

		@Bean
		AbandonedFirebaseEnrollmentCleanupWorker abandonedFirebaseEnrollmentCleanupWorker(
				AbandonedFirebaseEnrollmentCleanupRepository repository,
				AbandonedFirebaseEnrollmentTargetGuard targetGuard,
				AbandonedFirebaseUserCleanupPort cleanupPort,
				AbandonedFirebaseCleanupRetryPolicy retryPolicy,
				AbandonedFirebaseCleanupTerminalTransactionService terminalService,
				MeterRegistry meterRegistry,
				Clock clock,
				FirebaseAbandonedCleanupProperties properties
		) {
			return new AbandonedFirebaseEnrollmentCleanupWorker(
					repository, targetGuard, cleanupPort, retryPolicy, terminalService,
					meterRegistry, clock, properties.getLeaseDuration(), properties.getMaxAttempts()
			);
		}

		@Bean
		FirebaseAbandonedCleanupScheduler firebaseAbandonedCleanupScheduler(
				AbandonedFirebaseEnrollmentCleanupWorker worker,
				FirebaseAbandonedCleanupProperties properties
		) {
			return new FirebaseAbandonedCleanupScheduler(worker, properties.getMaxBatchSize());
		}
	}
}
