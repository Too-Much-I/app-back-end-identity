package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class UserWithdrawalExternalCleanupWorkerTests {

	private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");

	private UserWithdrawalLifecycleRepository repository;
	private WithdrawalCleanupTargetGuard targetGuard;
	private FirebaseWithdrawalCleanupPort cleanupPort;
	private UserWithdrawalExternalCleanupWorker worker;
	private UserWithdrawalLifecycle lifecycle;

	@BeforeEach
	void setUp() {
		repository = mock(UserWithdrawalLifecycleRepository.class);
		targetGuard = mock(WithdrawalCleanupTargetGuard.class);
		cleanupPort = mock(FirebaseWithdrawalCleanupPort.class);
		worker = worker(5);
		lifecycle = mock(UserWithdrawalLifecycle.class);
		when(lifecycle.getWithdrawalId()).thenReturn("withdrawal-id");
		when(lifecycle.getFirebaseProjectId()).thenReturn("test-project");
		when(lifecycle.getFirebaseUid()).thenReturn("opaque-uid");
		when(lifecycle.getLeaseOwner()).thenReturn("lease-token");
		when(lifecycle.getVersion()).thenReturn(7L);
		when(lifecycle.getAttemptCount()).thenReturn(1);
		when(repository.claimNext(any(), eq(NOW), eq(NOW.plusSeconds(30))))
				.thenReturn(Optional.of(lifecycle));
	}

	@Test
	void pendingLogoutActorWaitsWithoutCallingFirebase() {
		when(targetGuard.verify(lifecycle, NOW)).thenReturn(new WithdrawalCleanupTargetGuard.Result(
				WithdrawalCleanupTargetGuard.ResultType.LOGOUT_PENDING, null
		));
		when(repository.scheduleRetry(
				"withdrawal-id", "lease-token", 7L,
				WithdrawalCleanupFailureCode.LOGOUT_REVOKE_PENDING, NOW.plusSeconds(5), NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.RETRY_SCHEDULED);
		verifyNoInteractions(cleanupPort);
	}

	@Test
	void localTargetCompletesWithoutFirebaseCall() {
		when(targetGuard.verify(lifecycle, NOW)).thenReturn(new WithdrawalCleanupTargetGuard.Result(
				WithdrawalCleanupTargetGuard.ResultType.LOCAL_TARGET, null
		));
		when(repository.markExternalCleanupCompleted(
				"withdrawal-id", "lease-token", 7L, NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.LOCAL_TARGET_SKIPPED);
		verifyNoInteractions(cleanupPort);
	}

	@Test
	void targetMismatchMovesToReconciliationWithoutFirebaseCall() {
		when(targetGuard.verify(lifecycle, NOW)).thenReturn(new WithdrawalCleanupTargetGuard.Result(
				WithdrawalCleanupTargetGuard.ResultType.RECONCILIATION_REQUIRED,
				WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH
		));
		when(repository.markReconciliationRequired(
				"withdrawal-id", "lease-token", 7L,
				WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH, false, NOW
		)).thenReturn(true);

		assertThat(worker.processNext())
				.isEqualTo(WithdrawalCleanupOutcome.RECONCILIATION_REQUIRED);
		verifyNoInteractions(cleanupPort);
	}

	@Test
	void performsDisableRevokeProviderDeleteAndPresenceInOrder() {
		firebaseTarget();
		FirebaseCleanupAccountSnapshot snapshot = new FirebaseCleanupAccountSnapshot(
				false, Set.of(FirebaseCleanupProvider.GOOGLE)
		);
		when(cleanupPort.inspect("test-project", "opaque-uid")).thenReturn(snapshot);
		when(cleanupPort.checkPresence("test-project", "opaque-uid"))
				.thenReturn(FirebaseAccountPresence.ABSENT);
		when(repository.renewLease(eq("withdrawal-id"), eq("lease-token"), anyLong(),
				any(), eq(NOW))).thenReturn(true);
		when(repository.markExternalCleanupCompleted(
				"withdrawal-id", "lease-token", 10L, NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.EXTERNAL_DELETED);

		InOrder order = inOrder(cleanupPort);
		order.verify(cleanupPort).inspect("test-project", "opaque-uid");
		order.verify(cleanupPort).disable("test-project", "opaque-uid");
		order.verify(cleanupPort).revokeRefreshTokens("test-project", "opaque-uid");
		order.verify(cleanupPort).satisfyProviderDeletionObligations(snapshot);
		order.verify(cleanupPort).delete("test-project", "opaque-uid");
		order.verify(cleanupPort).checkPresence("test-project", "opaque-uid");
	}

	@Test
	void notFoundAtInspectConvergesToCompleted() {
		firebaseTarget();
		when(cleanupPort.inspect("test-project", "opaque-uid")).thenThrow(
				new FirebaseWithdrawalCleanupException(WithdrawalCleanupFailureCode.NOT_FOUND)
		);
		when(repository.markExternalCleanupCompleted(
				"withdrawal-id", "lease-token", 7L, NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.EXTERNAL_DELETED);
		verify(cleanupPort, never()).disable(any(), any());
	}

	@Test
	void deleteTimeoutUsesPresenceAndSucceedsWhenAccountIsAbsent() {
		prepareUntilDelete();
		doThrow(new FirebaseWithdrawalCleanupException(WithdrawalCleanupFailureCode.TIMEOUT))
				.when(cleanupPort).delete("test-project", "opaque-uid");
		when(cleanupPort.checkPresence("test-project", "opaque-uid"))
				.thenReturn(FirebaseAccountPresence.ABSENT);
		when(repository.markExternalCleanupCompleted(
				"withdrawal-id", "lease-token", 10L, NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.EXTERNAL_DELETED);
	}

	@Test
	void presentAfterDeleteSchedulesRetryWithSafeCode() {
		prepareUntilDelete();
		when(cleanupPort.checkPresence("test-project", "opaque-uid"))
				.thenReturn(FirebaseAccountPresence.PRESENT);
		when(repository.scheduleRetry(
				"withdrawal-id", "lease-token", 10L,
				WithdrawalCleanupFailureCode.DELETE_NOT_CONFIRMED,
				NOW.plusSeconds(5), NOW
		)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.RETRY_SCHEDULED);
	}

	@Test
	void maxAttemptsMovesRetryableFailureToReconciliation() {
		worker = worker(1);
		firebaseTarget();
		when(cleanupPort.inspect("test-project", "opaque-uid")).thenThrow(
				new FirebaseWithdrawalCleanupException(WithdrawalCleanupFailureCode.UNAVAILABLE)
		);
		when(repository.markReconciliationRequired(
				"withdrawal-id", "lease-token", 7L,
				WithdrawalCleanupFailureCode.UNAVAILABLE, true, NOW
		)).thenReturn(true);

		assertThat(worker.processNext())
				.isEqualTo(WithdrawalCleanupOutcome.RECONCILIATION_REQUIRED);
	}

	@Test
	void lostLeaseStopsBeforeNextRemoteMutation() {
		firebaseTarget();
		when(cleanupPort.inspect("test-project", "opaque-uid")).thenReturn(
				new FirebaseCleanupAccountSnapshot(false, Set.of())
		);
		when(repository.renewLease(
				"withdrawal-id", "lease-token", 7L, NOW.plusSeconds(30), NOW
		)).thenReturn(false);

		assertThat(worker.processNext()).isEqualTo(WithdrawalCleanupOutcome.LEASE_LOST);
		verify(cleanupPort, never()).revokeRefreshTokens(any(), any());
	}

	@Test
	void handoffUsesLocalAtomicRepositoryOperation() {
		when(repository.handoffNextCompleted(NOW)).thenReturn(Optional.of(lifecycle));

		assertThat(worker.handoffNextCompleted()).isEqualTo(WithdrawalCleanupOutcome.HANDED_OFF);
		verifyNoInteractions(cleanupPort);
	}

	private void firebaseTarget() {
		when(targetGuard.verify(lifecycle, NOW)).thenReturn(new WithdrawalCleanupTargetGuard.Result(
				WithdrawalCleanupTargetGuard.ResultType.FIREBASE_TARGET, null
		));
	}

	private void prepareUntilDelete() {
		firebaseTarget();
		when(cleanupPort.inspect("test-project", "opaque-uid")).thenReturn(
				new FirebaseCleanupAccountSnapshot(false, Set.of())
		);
		when(repository.renewLease(eq("withdrawal-id"), eq("lease-token"), anyLong(),
				any(), eq(NOW))).thenReturn(true);
	}

	private UserWithdrawalExternalCleanupWorker worker(int maxAttempts) {
		return new UserWithdrawalExternalCleanupWorker(
				repository,
				targetGuard,
				cleanupPort,
				new WithdrawalCleanupRetryPolicy(
						Duration.ofSeconds(5), Duration.ofMinutes(1), () -> 0.5d
				),
				new SimpleMeterRegistry(),
				Clock.fixed(NOW, ZoneOffset.UTC),
				Duration.ofSeconds(30),
				maxAttempts
		);
	}
}
