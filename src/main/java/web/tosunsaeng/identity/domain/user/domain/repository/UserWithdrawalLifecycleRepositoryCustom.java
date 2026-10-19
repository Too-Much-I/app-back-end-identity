package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

public interface UserWithdrawalLifecycleRepositoryCustom {

	Optional<UserWithdrawalLifecycle> claimNext(
			String leaseToken,
			Instant now,
			Instant leaseUntil
	);

	boolean renewLease(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			Instant renewedUntil,
			Instant updatedAt
	);

	boolean scheduleRetry(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			Instant nextAttemptAt,
			Instant updatedAt
	);

	boolean markReconciliationRequired(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			boolean maxAttemptsExceeded,
			Instant updatedAt
	);

	boolean markExternalCleanupCompleted(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			Instant externalDeletedAt
	);

	Optional<UserWithdrawalLifecycle> handoffNextCompleted(Instant updatedAt);

	boolean markIdentityReleaseCleaned(
			String withdrawalId,
			long expectedVersion,
			Instant releasedAt
	);

	boolean markIdentityReleaseReconciliationRequired(
			String withdrawalId,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			Instant updatedAt
	);
}
