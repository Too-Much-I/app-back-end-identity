package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;

class AbandonedFirebaseEnrollmentCleanupWorkerTests {

	private static final Instant NOW = Instant.parse("2026-08-28T00:00:00Z");
	private final AbandonedFirebaseEnrollmentCleanupRepository repository = mock(
			AbandonedFirebaseEnrollmentCleanupRepository.class
	);
	private final AbandonedFirebaseEnrollmentTargetGuard guard = mock(
			AbandonedFirebaseEnrollmentTargetGuard.class
	);
	private final AbandonedFirebaseUserCleanupPort port = mock(
			AbandonedFirebaseUserCleanupPort.class
	);
	private final AbandonedFirebaseCleanupTerminalTransactionService terminal = mock(
			AbandonedFirebaseCleanupTerminalTransactionService.class
	);
	private final AbandonedFirebaseEnrollmentCleanup lifecycle = mock(
			AbandonedFirebaseEnrollmentCleanup.class
	);
	private final AbandonedFirebaseEnrollmentCleanupWorker worker =
			new AbandonedFirebaseEnrollmentCleanupWorker(
					repository, guard, port,
					new AbandonedFirebaseCleanupRetryPolicy(
							Duration.ofSeconds(5), Duration.ofHours(1), () -> 0.5
					),
					terminal, new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC),
					Duration.ofMinutes(1), 12
			);

	@BeforeEach
	void claim() {
		when(repository.claimNext(any(), eq(NOW), eq(NOW.plus(Duration.ofMinutes(1)))))
				.thenReturn(Optional.of(lifecycle));
		when(lifecycle.getCleanupId()).thenReturn("cleanup-id");
		when(lifecycle.getFirebaseProjectId()).thenReturn("project");
		when(lifecycle.getFirebaseUid()).thenReturn("uid");
		when(lifecycle.getGeneration()).thenReturn(2L);
		when(lifecycle.getLeaseOwner()).thenReturn("lease");
		when(lifecycle.getVersion()).thenReturn(3L);
		when(lifecycle.getAttemptCount()).thenReturn(1);
	}

	@Test
	void runsMutationOrderAndCompletesOnlyAfterAbsenceConfirmation() {
		AbandonedFirebaseAccountSnapshot snapshot =
				new AbandonedFirebaseAccountSnapshot(false, null, List.of());
		when(guard.verifyLocal(lifecycle, NOW))
				.thenReturn(AbandonedFirebaseEnrollmentTargetGuard.Result.allowedResult());
		when(port.inspect("project", "uid")).thenReturn(snapshot);
		when(guard.verifyExternalOwners(snapshot))
				.thenReturn(AbandonedFirebaseEnrollmentTargetGuard.Result.allowedResult());
		when(repository.renewLease(
				any(), any(), anyLong(), anyLong(), any(), any()
		)).thenReturn(true);
		when(port.checkPresence("project", "uid"))
				.thenReturn(AbandonedFirebaseAccountPresence.ABSENT);
		when(terminal.markCleaned(lifecycle, 7L, NOW)).thenReturn(true);

		assertThat(worker.processNext()).isEqualTo(AbandonedFirebaseCleanupOutcome.CLEANED);

		InOrder order = inOrder(port);
		order.verify(port).inspect("project", "uid");
		order.verify(port).disable("project", "uid");
		order.verify(port).revokeRefreshTokens("project", "uid");
		order.verify(port).satisfyProviderDeletionObligations(snapshot);
		order.verify(port).delete("project", "uid");
		order.verify(port).checkPresence("project", "uid");
	}

	@Test
	void ownerConflictMovesToReconciliationWithoutFirebaseMutation() {
		when(guard.verifyLocal(lifecycle, NOW)).thenReturn(
				AbandonedFirebaseEnrollmentTargetGuard.Result.reconcile(
						AbandonedFirebaseEnrollmentFailureCode.OWNER_PRESENT
				)
		);
		when(repository.markReconciliationRequired(
				"cleanup-id", "lease", 2L, 3L,
				AbandonedFirebaseEnrollmentFailureCode.OWNER_PRESENT, NOW
		)).thenReturn(true);

		assertThat(worker.processNext())
				.isEqualTo(AbandonedFirebaseCleanupOutcome.RECONCILIATION_REQUIRED);
		verify(port, never()).inspect(any(), any());
	}

	@Test
	void retryableInspectFailureSchedulesBoundedBackoff() {
		when(guard.verifyLocal(lifecycle, NOW))
				.thenReturn(AbandonedFirebaseEnrollmentTargetGuard.Result.allowedResult());
		when(port.inspect("project", "uid")).thenThrow(
				new AbandonedFirebaseCleanupException(
						AbandonedFirebaseEnrollmentFailureCode.UNAVAILABLE
				)
		);
		when(repository.scheduleRetry(
				"cleanup-id", "lease", 2L, 3L,
				AbandonedFirebaseEnrollmentFailureCode.UNAVAILABLE,
				NOW.plusSeconds(5), NOW
		)).thenReturn(true);

		assertThat(worker.processNext())
				.isEqualTo(AbandonedFirebaseCleanupOutcome.RETRY_SCHEDULED);
	}
}
