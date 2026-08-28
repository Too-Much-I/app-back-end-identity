package web.tosunsaeng.identity.domain.auth.federation.repository;

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

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;

public class AbandonedFirebaseEnrollmentCleanupRepositoryImpl
		implements AbandonedFirebaseEnrollmentCleanupRepositoryCustom {

	private final MongoOperations mongoOperations;

	public AbandonedFirebaseEnrollmentCleanupRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
	}

	@Override
	public Optional<AbandonedFirebaseEnrollmentCleanup> claimNext(
			String leaseToken,
			Instant now,
			Instant leaseUntil
	) {
		String requiredToken = requireText(leaseToken, "leaseToken");
		Instant requiredNow = Objects.requireNonNull(now);
		Instant requiredLeaseUntil = Objects.requireNonNull(leaseUntil);
		if (!requiredLeaseUntil.isAfter(requiredNow)) {
			throw new IllegalArgumentException("leaseUntil must be after now");
		}
		Criteria graceDue = new Criteria().andOperator(
				Criteria.where("status").is(AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE),
				Criteria.where("graceUntil").lte(requiredNow)
		);
		Criteria retryDue = new Criteria().andOperator(
				Criteria.where("status").is(AbandonedFirebaseEnrollmentCleanupStatus.RETRY_WAIT),
				Criteria.where("nextAttemptAt").lte(requiredNow)
		);
		Criteria expiredLease = new Criteria().andOperator(
				Criteria.where("status").is(
						AbandonedFirebaseEnrollmentCleanupStatus.CLEANUP_IN_PROGRESS
				),
				Criteria.where("leaseUntil").lte(requiredNow)
		);
		Query query = Query.query(new Criteria().orOperator(
				graceDue, retryDue, expiredLease
		)).with(Sort.by(Sort.Direction.ASC, "graceUntil", "createdAt", "_id"));
		Update update = new Update()
				.set("status", AbandonedFirebaseEnrollmentCleanupStatus.CLEANUP_IN_PROGRESS)
				.set("leaseOwner", requiredToken)
				.set("leaseUntil", requiredLeaseUntil)
				.set("updatedAt", requiredNow)
				.unset("nextAttemptAt")
				.unset("lastErrorCode")
				.inc("attemptCount", 1)
				.inc("version", 1L);
		return Optional.ofNullable(mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				AbandonedFirebaseEnrollmentCleanup.class
		));
	}

	@Override
	public boolean renewLease(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			Instant renewedUntil,
			Instant updatedAt
	) {
		Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt);
		Instant requiredRenewedUntil = Objects.requireNonNull(renewedUntil);
		if (!requiredRenewedUntil.isAfter(requiredUpdatedAt)) {
			throw new IllegalArgumentException("renewedUntil must be after updatedAt");
		}
		Query query = leasedQuery(cleanupId, leaseToken, generation, expectedVersion)
				.addCriteria(Criteria.where("leaseUntil").gt(requiredUpdatedAt));
		return updateOne(query, new Update()
				.set("leaseUntil", requiredRenewedUntil)
				.set("updatedAt", requiredUpdatedAt)
				.inc("version", 1L));
	}

	@Override
	public boolean scheduleRetry(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			AbandonedFirebaseEnrollmentFailureCode failureCode,
			Instant nextAttemptAt,
			Instant updatedAt
	) {
		AbandonedFirebaseEnrollmentFailureCode requiredCode = Objects.requireNonNull(failureCode);
		if (!requiredCode.isRetryable()) {
			throw new IllegalArgumentException("failureCode must be retryable");
		}
		Instant requiredUpdatedAt = Objects.requireNonNull(updatedAt);
		Instant requiredNext = Objects.requireNonNull(nextAttemptAt);
		if (!requiredNext.isAfter(requiredUpdatedAt)) {
			throw new IllegalArgumentException("nextAttemptAt must be after updatedAt");
		}
		return updateOne(
				leasedQuery(cleanupId, leaseToken, generation, expectedVersion)
						.addCriteria(Criteria.where("leaseUntil").gt(requiredUpdatedAt)),
				clearLease(requiredUpdatedAt)
						.set("status", AbandonedFirebaseEnrollmentCleanupStatus.RETRY_WAIT)
						.set("lastErrorCode", requiredCode)
						.set("nextAttemptAt", requiredNext)
		);
	}

	@Override
	public boolean markReconciliationRequired(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			AbandonedFirebaseEnrollmentFailureCode failureCode,
			Instant updatedAt
	) {
		return updateOne(
				leasedQuery(cleanupId, leaseToken, generation, expectedVersion)
						.addCriteria(Criteria.where("leaseUntil").gt(
								Objects.requireNonNull(updatedAt)
						)),
				clearLease(updatedAt)
						.set("status", AbandonedFirebaseEnrollmentCleanupStatus.RECONCILIATION_REQUIRED)
						.set("lastErrorCode", Objects.requireNonNull(failureCode))
						.unset("nextAttemptAt")
						.unset("cleanupAt")
		);
	}

	@Override
	public boolean markCleaned(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion,
			Instant terminalAt,
			Instant cleanupAt
	) {
		Instant requiredTerminalAt = Objects.requireNonNull(terminalAt);
		Instant requiredCleanupAt = requireAfter(cleanupAt, requiredTerminalAt, "cleanupAt");
		return updateOne(
				leasedQuery(cleanupId, leaseToken, generation, expectedVersion)
						.addCriteria(Criteria.where("leaseUntil").gt(requiredTerminalAt)),
				clearLease(requiredTerminalAt)
						.set("status", AbandonedFirebaseEnrollmentCleanupStatus.CLEANED)
						.set("externalDeletedAt", requiredTerminalAt)
						.set("completedAt", requiredTerminalAt)
						.set("cleanupAt", requiredCleanupAt)
						.unset("lastErrorCode")
						.unset("nextAttemptAt")
		);
	}

	@Override
	public boolean markFinalizedIfResumable(
			String firebaseProjectId,
			String firebaseUid,
			String sourceEnrollmentId,
			Instant terminalAt,
			Instant cleanupAt
	) {
		Instant requiredTerminalAt = Objects.requireNonNull(terminalAt);
		Instant requiredCleanupAt = requireAfter(cleanupAt, requiredTerminalAt, "cleanupAt");
		Query query = Query.query(Criteria.where("firebaseProjectId")
				.is(requireText(firebaseProjectId, "firebaseProjectId"))
				.and("firebaseUid").is(requireText(firebaseUid, "firebaseUid"))
				.and("sourceEnrollmentId").is(requireText(sourceEnrollmentId, "sourceEnrollmentId"))
				.and("status").is(AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE));
		Update update = new Update()
				.set("status", AbandonedFirebaseEnrollmentCleanupStatus.FINALIZED)
				.set("completedAt", requiredTerminalAt)
				.set("cleanupAt", requiredCleanupAt)
				.set("updatedAt", requiredTerminalAt)
				.unset("nextAttemptAt")
				.unset("leaseOwner")
				.unset("leaseUntil")
				.unset("lastErrorCode")
				.inc("version", 1L);
		return updateOne(query, update);
	}

	private Query leasedQuery(
			String cleanupId,
			String leaseToken,
			long generation,
			long expectedVersion
	) {
		if (generation < 1 || expectedVersion < 0) {
			throw new IllegalArgumentException("generation and version must be valid");
		}
		return Query.query(Criteria.where("_id")
				.is(requireText(cleanupId, "cleanupId"))
				.and("status").is(
						AbandonedFirebaseEnrollmentCleanupStatus.CLEANUP_IN_PROGRESS
				)
				.and("leaseOwner").is(requireText(leaseToken, "leaseToken"))
				.and("generation").is(generation)
				.and("version").is(expectedVersion));
	}

	private static Update clearLease(Instant updatedAt) {
		return new Update()
				.set("updatedAt", Objects.requireNonNull(updatedAt))
				.unset("leaseOwner")
				.unset("leaseUntil")
				.inc("version", 1L);
	}

	private boolean updateOne(Query query, Update update) {
		UpdateResult result = mongoOperations.updateFirst(
				query, update, AbandonedFirebaseEnrollmentCleanup.class
		);
		return result.getModifiedCount() == 1;
	}

	private static Instant requireAfter(Instant value, Instant boundary, String fieldName) {
		Instant required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (!required.isAfter(boundary)) {
			throw new IllegalArgumentException(fieldName + " must be after terminalAt");
		}
		return required;
	}

	private static String requireText(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank()) throw new IllegalArgumentException(fieldName + " must not be blank");
		return required;
	}
}
