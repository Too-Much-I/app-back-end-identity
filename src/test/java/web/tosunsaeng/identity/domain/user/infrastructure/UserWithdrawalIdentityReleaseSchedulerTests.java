package web.tosunsaeng.identity.domain.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.user.application.IdentityReleaseOutcome;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseTransactionService;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseWorker;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalIdentityReleaseSchedulerTests {

	@Test
	void defaultDisabledConfigurationDoesNotCreateReleaseComponents() {
		new ApplicationContextRunner()
				.withUserConfiguration(UserWithdrawalIdentityReleaseConfiguration.class)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(
							UserWithdrawalIdentityReleaseTransactionService.class
					);
					assertThat(context).doesNotHaveBean(
							UserWithdrawalIdentityReleaseWorker.class
					);
					assertThat(context).doesNotHaveBean(
							UserWithdrawalIdentityReleaseScheduler.class
					);
				});
	}

	@Test
	void enabledConfigurationCreatesReleaseComponents() {
		new ApplicationContextRunner()
				.withUserConfiguration(UserWithdrawalIdentityReleaseConfiguration.class)
				.withPropertyValues(
						"app.firebase-withdrawal-identity-release.enabled=true",
						"app.firebase-withdrawal-identity-release.fixed-delay=PT1H",
						"app.firebase-withdrawal-identity-release.max-batch-size=5"
				)
				.withBean(UserWithdrawalLifecycleRepository.class,
						() -> mock(UserWithdrawalLifecycleRepository.class))
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(FirebaseIdentityRepository.class,
						() -> mock(FirebaseIdentityRepository.class))
				.withBean(SocialIdentityRepository.class,
						() -> mock(SocialIdentityRepository.class))
				.withBean(PhoneIdentityRepository.class,
						() -> mock(PhoneIdentityRepository.class))
				.withBean(PhoneFingerprintAliasRepository.class,
						() -> mock(PhoneFingerprintAliasRepository.class))
				.withBean(PhoneEligibilityBindingRevisionRepository.class,
						() -> mock(PhoneEligibilityBindingRevisionRepository.class))
				.withBean(PhoneEligibilityBindingOutboxRepository.class,
						() -> mock(PhoneEligibilityBindingOutboxRepository.class))
				.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
				.withBean(Clock.class, Clock::systemUTC)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(
							UserWithdrawalIdentityReleaseTransactionService.class
					);
					assertThat(context).hasSingleBean(
							UserWithdrawalIdentityReleaseWorker.class
					);
					assertThat(context).hasSingleBean(
							UserWithdrawalIdentityReleaseScheduler.class
					);
				});
	}

	@Test
	void processesOnlyConfiguredBatchSize() {
		UserWithdrawalIdentityReleaseWorker worker = mock(
				UserWithdrawalIdentityReleaseWorker.class
		);
		when(worker.processNext()).thenReturn(IdentityReleaseOutcome.CLEANED);
		UserWithdrawalIdentityReleaseScheduler scheduler =
				new UserWithdrawalIdentityReleaseScheduler(worker, 3);

		scheduler.processBatch();

		verify(worker, times(3)).processNext();
	}

	@Test
	void stopsBatchWhenNoCandidateRemains() {
		UserWithdrawalIdentityReleaseWorker worker = mock(
				UserWithdrawalIdentityReleaseWorker.class
		);
		when(worker.processNext())
				.thenReturn(IdentityReleaseOutcome.CLEANED)
				.thenReturn(IdentityReleaseOutcome.NONE);
		UserWithdrawalIdentityReleaseScheduler scheduler =
				new UserWithdrawalIdentityReleaseScheduler(worker, 5);

		scheduler.processBatch();

		verify(worker, times(2)).processNext();
	}

	@Test
	void stopsBatchAfterTransientFailure() {
		UserWithdrawalIdentityReleaseWorker worker = mock(
				UserWithdrawalIdentityReleaseWorker.class
		);
		when(worker.processNext()).thenReturn(IdentityReleaseOutcome.TRANSIENT_FAILURE);
		UserWithdrawalIdentityReleaseScheduler scheduler =
				new UserWithdrawalIdentityReleaseScheduler(worker, 5);

		scheduler.processBatch();

		verify(worker).processNext();
	}
}
