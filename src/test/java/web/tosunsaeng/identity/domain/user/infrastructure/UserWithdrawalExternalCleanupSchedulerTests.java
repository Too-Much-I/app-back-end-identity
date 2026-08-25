package web.tosunsaeng.identity.domain.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseCredentialsProvider;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseWithdrawalCleanupAppHandle;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.application.FirebaseWithdrawalCleanupPort;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalExternalCleanupWorker;
import web.tosunsaeng.identity.domain.user.application.WithdrawalCleanupOutcome;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalExternalCleanupSchedulerTests {

	@Test
	void defaultDisabledConfigurationDoesNotCreateWorkerOrScheduler() {
		new ApplicationContextRunner()
				.withUserConfiguration(UserWithdrawalExternalCleanupConfiguration.class)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(UserWithdrawalExternalCleanupWorker.class);
					assertThat(context).doesNotHaveBean(
							UserWithdrawalExternalCleanupScheduler.class
					);
				});
	}

	@Test
	void enabledConfigurationCreatesDedicatedCleanupComponents() {
		UserWithdrawalLifecycleRepository lifecycleRepository = mock(
				UserWithdrawalLifecycleRepository.class
		);
		when(lifecycleRepository.handoffNextCompleted(any(Instant.class))).thenReturn(Optional.empty());
		when(lifecycleRepository.claimNext(anyString(), any(Instant.class), any(Instant.class)))
				.thenReturn(Optional.empty());

		new ApplicationContextRunner()
				.withUserConfiguration(UserWithdrawalExternalCleanupConfiguration.class)
				.withPropertyValues(
						"app.firebase-withdrawal-cleanup.enabled=true",
						"app.firebase-withdrawal-cleanup.fixed-delay=PT1H"
				)
				.withBean(FirebaseAuthProperties.class, () -> firebaseProperties(true))
				.withBean(FirebaseCredentialsProvider.class, () -> this::fakeCredentials)
				.withBean(UserWithdrawalLifecycleRepository.class, () -> lifecycleRepository)
				.withBean(UserRepository.class, () -> mock(UserRepository.class))
				.withBean(FirebaseIdentityRepository.class,
						() -> mock(FirebaseIdentityRepository.class))
				.withBean(MeterRegistry.class, SimpleMeterRegistry::new)
				.withBean(Clock.class, Clock::systemUTC)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(FirebaseWithdrawalCleanupAppHandle.class);
					assertThat(context).hasSingleBean(FirebaseWithdrawalCleanupPort.class);
					assertThat(context).hasSingleBean(UserWithdrawalExternalCleanupWorker.class);
					assertThat(context).hasSingleBean(
							UserWithdrawalExternalCleanupScheduler.class
					);
				});
	}

	@Test
	void enabledCleanupFailsClosedWhenFirebaseIsDisabled() {
		new ApplicationContextRunner()
				.withUserConfiguration(UserWithdrawalExternalCleanupConfiguration.class)
				.withPropertyValues("app.firebase-withdrawal-cleanup.enabled=true")
				.withBean(FirebaseAuthProperties.class, () -> firebaseProperties(false))
				.withBean(FirebaseCredentialsProvider.class, () -> this::fakeCredentials)
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void processesCompletedHandoffsBeforeExternalClaimsAndStopsAtNone() {
		UserWithdrawalExternalCleanupWorker worker = mock(
				UserWithdrawalExternalCleanupWorker.class
		);
		when(worker.handoffNextCompleted()).thenReturn(
				WithdrawalCleanupOutcome.HANDED_OFF,
				WithdrawalCleanupOutcome.NONE
		);
		when(worker.processNext()).thenReturn(
				WithdrawalCleanupOutcome.EXTERNAL_DELETED,
				WithdrawalCleanupOutcome.NONE
		);
		UserWithdrawalExternalCleanupScheduler scheduler =
				new UserWithdrawalExternalCleanupScheduler(worker, 10);

		scheduler.processBatch();

		verify(worker, times(2)).handoffNextCompleted();
		verify(worker, times(2)).processNext();
	}

	@Test
	void batchLimitCapsCombinedHandoffAndExternalWork() {
		UserWithdrawalExternalCleanupWorker worker = mock(
				UserWithdrawalExternalCleanupWorker.class
		);
		when(worker.handoffNextCompleted()).thenReturn(WithdrawalCleanupOutcome.HANDED_OFF);
		UserWithdrawalExternalCleanupScheduler scheduler =
				new UserWithdrawalExternalCleanupScheduler(worker, 2);

		scheduler.processBatch();

		verify(worker, times(2)).handoffNextCompleted();
		verify(worker, times(0)).processNext();
	}

	private FirebaseAuthProperties firebaseProperties(boolean enabled) {
		return new FirebaseAuthProperties(
				enabled,
				enabled ? "test-project" : null,
				null,
				true,
				false,
				false,
				true,
				"oidc.kakao",
				Duration.ofMinutes(15),
				Duration.ofMinutes(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(3),
				Duration.ofSeconds(5),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}

	private GoogleCredentials fakeCredentials() {
		return GoogleCredentials.create(new AccessToken(
				"test-only-cleanup-configuration-token",
				Date.from(Instant.parse("2099-01-01T00:00:00Z"))
		));
	}
}
