package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalIdentityReleaseWorkerTests {

	private static final Instant NOW = Instant.parse("2026-08-26T03:00:00Z");

	private UserWithdrawalLifecycleRepository repository;
	private UserWithdrawalIdentityReleaseTransactionService transactionService;
	private UserWithdrawalIdentityReleaseWorker worker;
	private UserWithdrawalLifecycle lifecycle;

	@BeforeEach
	void setUp() {
		repository = mock(UserWithdrawalLifecycleRepository.class);
		transactionService = mock(UserWithdrawalIdentityReleaseTransactionService.class);
		worker = new UserWithdrawalIdentityReleaseWorker(
				repository,
				transactionService,
				new SimpleMeterRegistry(),
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		lifecycle = mock(UserWithdrawalLifecycle.class);
	}

	@Test
	void pendingLogoutActorKeepsReleasePending() {
		when(repository.findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
				UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
		)).thenReturn(Optional.of(lifecycle));
		when(transactionService.release(lifecycle, NOW)).thenThrow(
				new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING)
		);

		assertThat(worker.processNext()).isEqualTo(IdentityReleaseOutcome.DEPENDENCY_PENDING);
	}

	@Test
	void returnsNoneWhenNoPendingLifecycleExists() {
		when(repository.findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
				UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
		)).thenReturn(Optional.empty());

		assertThat(worker.processNext()).isEqualTo(IdentityReleaseOutcome.NONE);
		verifyNoInteractions(transactionService);
	}

	@Test
	void delegatesSelectedLifecycleToTransactionalRelease() {
		when(repository.findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
				UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
		)).thenReturn(Optional.of(lifecycle));
		when(transactionService.release(lifecycle, NOW))
				.thenReturn(IdentityReleaseOutcome.CLEANED);

		assertThat(worker.processNext()).isEqualTo(IdentityReleaseOutcome.CLEANED);
	}

	@Test
	void concurrentTransactionLoserStopsCurrentBatchWithoutMutationGuessing() {
		when(repository.findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
				UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
		)).thenReturn(Optional.of(lifecycle));
		when(transactionService.release(lifecycle, NOW)).thenThrow(
				new OptimisticLockingFailureException("lost")
		);

		assertThat(worker.processNext()).isEqualTo(IdentityReleaseOutcome.CONCURRENT_CHANGE);
	}
}
