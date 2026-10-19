package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;

public interface AbandonedFirebaseEnrollmentCleanupRepositoryCustom {

	Optional<AbandonedFirebaseEnrollmentCleanup> claimNext(
			String leaseToken,
			Instant now,
			Instant leaseUntil
	);

	boolean renewLease(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			Instant renewedUntil,
			Instant updatedAt
	);

	boolean scheduleRetry(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			AbandonedFirebaseEnrollmentFailureCode failureCode,
			Instant nextAttemptAt,
			Instant updatedAt
	);

	boolean markReconciliationRequired(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			AbandonedFirebaseEnrollmentFailureCode failureCode,
			Instant updatedAt
	);

	boolean markCleaned(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			Instant terminalAt,
			Instant cleanupAt
	);

	boolean markFinalizedIfResumable(
			String firebaseProjectId,
			String firebaseUid,
			String sourceEnrollmentId,
			Instant terminalAt,
			Instant cleanupAt
	);
}
