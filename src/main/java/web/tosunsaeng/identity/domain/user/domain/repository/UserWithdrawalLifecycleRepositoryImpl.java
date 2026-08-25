package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

public class UserWithdrawalLifecycleRepositoryImpl
		implements UserWithdrawalLifecycleRepositoryCustom {

	private final MongoOperations mongoOperations;

	public UserWithdrawalLifecycleRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(
				mongoOperations,
				"mongoOperations must not be null"
		);
	}

	@Override
	public Optional<UserWithdrawalLifecycle> claimNext(
			String leaseToken,
			Instant now,
			Instant leaseUntil
	) {
		String requiredToken = requireText(leaseToken, "leaseToken");
		Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
		Instant requiredLeaseUntil = Objects.requireNonNull(
				leaseUntil,
				"leaseUntil must not be null"
		);
		if (!requiredLeaseUntil.isAfter(requiredNow)) {
			throw new IllegalArgumentException("leaseUntil must be after now");
		}

		Criteria pendingDue = new Criteria().andOperator(
				Criteria.where("status").is(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING),
				Criteria.where("nextAttemptAt").lte(requiredNow)
		);
		Criteria retryDue = new Criteria().andOperator(
				Criteria.where("status").is(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_RETRY_WAIT),
				Criteria.where("nextAttemptAt").lte(requiredNow)
		);
		Criteria expiredLease = new Criteria().andOperator(
				Criteria.where("status").is(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS),
				Criteria.where("leaseUntil").lte(requiredNow)
		);
		Query query = Query.query(new Criteria().orOperator(
				pendingDue,
				retryDue,
				expiredLease
		)).with(Sort.by(Sort.Direction.ASC, "requestedAt", "_id"));
		Update update = new Update()
				.set("status", UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS)
				.set("leaseOwner", requiredToken)
				.set("leaseUntil", requiredLeaseUntil)
				.set("updatedAt", requiredNow)
				.unset("nextAttemptAt")
				.unset("lastErrorCode")
				.set("maxAttemptsExceeded", false)
				.inc("attemptCount", 1)
				.inc("version", 1L);
		return Optional.ofNullable(mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				UserWithdrawalLifecycle.class
		));
	}

	@Override
	public boolean renewLease(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			Instant renewedUntil,
			Instant updatedAt
	) {
		Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		Instant requiredRenewedUntil = Objects.requireNonNull(
				renewedUntil,
				"renewedUntil must not be null"
		);
		if (!requiredRenewedUntil.isAfter(requiredUpdatedAt)) {
			throw new IllegalArgumentException("renewedUntil must be after updatedAt");
		}
		Query query = leasedQuery(withdrawalId, leaseToken, expectedVersion)
				.addCriteria(Criteria.where("leaseUntil").gt(requiredUpdatedAt));
		Update update = new Update()
				.set("leaseUntil", requiredRenewedUntil)
				.set("updatedAt", requiredUpdatedAt)
				.inc("version", 1L);
		return updateOne(query, update);
	}

	@Override
	public boolean scheduleRetry(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			Instant nextAttemptAt,
			Instant updatedAt
	) {
		WithdrawalCleanupFailureCode requiredCode = Objects.requireNonNull(
				failureCode,
				"failureCode must not be null"
		);
		if (!requiredCode.isRetryable()) {
			throw new IllegalArgumentException("failureCode must be retryable");
		}
		Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		Instant requiredNextAttemptAt = Objects.requireNonNull(
				nextAttemptAt,
				"nextAttemptAt must not be null"
		);
		if (!requiredNextAttemptAt.isAfter(requiredUpdatedAt)) {
			throw new IllegalArgumentException("nextAttemptAt must be after updatedAt");
		}
		Update update = terminalClaimUpdate(requiredUpdatedAt)
				.set("status", UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_RETRY_WAIT)
				.set("lastErrorCode", requiredCode.name())
				.set("maxAttemptsExceeded", false)
				.set("nextAttemptAt", requiredNextAttemptAt);
		return updateOne(leasedQuery(withdrawalId, leaseToken, expectedVersion), update);
	}

	@Override
	public boolean markReconciliationRequired(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			boolean maxAttemptsExceeded,
			Instant updatedAt
	) {
		WithdrawalCleanupFailureCode requiredCode = Objects.requireNonNull(
				failureCode,
				"failureCode must not be null"
		);
		Update update = terminalClaimUpdate(updatedAt)
				.set("status", UserWithdrawalCleanupStatus.RECONCILIATION_REQUIRED)
				.set("lastErrorCode", requiredCode.name())
				.set("maxAttemptsExceeded", maxAttemptsExceeded)
				.unset("nextAttemptAt");
		return updateOne(leasedQuery(withdrawalId, leaseToken, expectedVersion), update);
	}

	@Override
	public boolean markExternalCleanupCompleted(
			String withdrawalId,
			String leaseToken,
			long expectedVersion,
			Instant externalDeletedAt
	) {
		Instant requiredDeletedAt = Objects.requireNonNull(
				externalDeletedAt,
				"externalDeletedAt must not be null"
		);
		Update update = terminalClaimUpdate(requiredDeletedAt)
				.set("status", UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_COMPLETED)
				.set("externalDeletedAt", requiredDeletedAt)
				.set("maxAttemptsExceeded", false)
				.unset("lastErrorCode")
				.unset("nextAttemptAt");
		return updateOne(leasedQuery(withdrawalId, leaseToken, expectedVersion), update);
	}

	@Override
	public Optional<UserWithdrawalLifecycle> handoffNextCompleted(Instant updatedAt) {
		Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		Query query = Query.query(Criteria.where("status")
				.is(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_COMPLETED))
				.with(Sort.by(Sort.Direction.ASC, "externalDeletedAt", "_id"));
		Update update = new Update()
				.set("status", UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING)
				.set("updatedAt", requiredUpdatedAt)
				.inc("version", 1L);
		return Optional.ofNullable(mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				UserWithdrawalLifecycle.class
		));
	}

	private Query leasedQuery(String withdrawalId, String leaseToken, long expectedVersion) {
		if (expectedVersion < 0) {
			throw new IllegalArgumentException("expectedVersion must not be negative");
		}
		return Query.query(Criteria.where("_id")
				.is(requireText(withdrawalId, "withdrawalId"))
				.and("status").is(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS)
				.and("leaseOwner").is(requireText(leaseToken, "leaseToken"))
				.and("version").is(expectedVersion));
	}

	private Update terminalClaimUpdate(Instant updatedAt) {
		return new Update()
				.set("updatedAt", Objects.requireNonNull(updatedAt, "updatedAt must not be null"))
				.unset("leaseOwner")
				.unset("leaseUntil")
				.inc("version", 1L);
	}

	private boolean updateOne(Query query, Update update) {
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				UserWithdrawalLifecycle.class
		);
		return result.getModifiedCount() == 1;
	}

	private static String requireText(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return required;
	}
}
