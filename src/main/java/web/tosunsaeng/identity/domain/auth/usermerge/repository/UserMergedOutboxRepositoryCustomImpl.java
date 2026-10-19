package web.tosunsaeng.identity.domain.auth.usermerge.repository;

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

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedOutboxStatus;

public final class UserMergedOutboxRepositoryCustomImpl
		implements UserMergedOutboxRepositoryCustom {

	private final MongoOperations mongoOperations;

	public UserMergedOutboxRepositoryCustomImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
	}

	@Override
	public Optional<UserMergedOutbox> claimNext(
			String leaseOwner,
			Instant claimedAt,
			Instant leaseExpiresAt
	) {
		String requiredOwner = requireText(leaseOwner, "leaseOwner");
		Instant requiredClaimedAt = Objects.requireNonNull(claimedAt);
		Instant requiredLeaseExpiresAt = Objects.requireNonNull(leaseExpiresAt);
		if (!requiredLeaseExpiresAt.isAfter(requiredClaimedAt)) {
			throw new IllegalArgumentException("leaseExpiresAt must be after claimedAt");
		}
		Criteria pendingDue = new Criteria().andOperator(
				Criteria.where("status").is(UserMergedOutboxStatus.PENDING),
				Criteria.where("nextAttemptAt").lte(requiredClaimedAt)
		);
		Criteria expiredLease = new Criteria().andOperator(
				Criteria.where("status").is(UserMergedOutboxStatus.IN_FLIGHT),
				Criteria.where("leaseExpiresAt").lte(requiredClaimedAt)
		);
		Query query = Query.query(new Criteria().orOperator(pendingDue, expiredLease))
				.with(Sort.by(Sort.Direction.ASC, "occurredAt", "eventId"));
		Update update = new Update()
				.set("status", UserMergedOutboxStatus.IN_FLIGHT)
				.set("leaseOwner", requiredOwner)
				.set("leaseExpiresAt", requiredLeaseExpiresAt)
				.inc("attemptCount", 1L);
		return Optional.ofNullable(mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				UserMergedOutbox.class
		));
	}

	@Override
	public boolean markPublished(
			String eventId,
			String leaseOwner,
			Instant publishedAt,
			Instant cleanupAt
	) {
		return updateLeased(eventId, leaseOwner, new Update()
				.set("status", UserMergedOutboxStatus.PUBLISHED)
				.set("publishedAt", Objects.requireNonNull(publishedAt))
				.set("cleanupAt", Objects.requireNonNull(cleanupAt))
				.unset("leaseOwner")
				.unset("leaseExpiresAt")
				.unset("nextAttemptAt")
				.unset("lastFailureCode"));
	}

	@Override
	public boolean scheduleRetry(
			String eventId,
			String leaseOwner,
			UserMergedFailureCode failureCode,
			Instant nextAttemptAt
	) {
		return updateLeased(eventId, leaseOwner, new Update()
				.set("status", UserMergedOutboxStatus.PENDING)
				.set("lastFailureCode", Objects.requireNonNull(failureCode))
				.set("nextAttemptAt", Objects.requireNonNull(nextAttemptAt))
				.unset("leaseOwner")
				.unset("leaseExpiresAt"));
	}

	@Override
	public boolean markDeadLetter(
			String eventId,
			String leaseOwner,
			UserMergedFailureCode failureCode,
			Instant deadLetteredAt,
			Instant retentionReviewAt
	) {
		return updateLeased(eventId, leaseOwner, new Update()
				.set("status", UserMergedOutboxStatus.DEAD_LETTER)
				.set("lastFailureCode", Objects.requireNonNull(failureCode))
				.set("deadLetteredAt", Objects.requireNonNull(deadLetteredAt))
				.set("retentionReviewAt", Objects.requireNonNull(retentionReviewAt))
				.unset("leaseOwner")
				.unset("leaseExpiresAt")
				.unset("nextAttemptAt"));
	}

	@Override
	public boolean replayDeadLetter(String eventId, Instant replayAt) {
		Query query = Query.query(Criteria.where("_id")
				.is(requireText(eventId, "eventId"))
				.and("status").is(UserMergedOutboxStatus.DEAD_LETTER));
		Update update = new Update()
				.set("status", UserMergedOutboxStatus.PENDING)
				.set("attemptCount", 0)
				.set("nextAttemptAt", Objects.requireNonNull(replayAt))
				.unset("lastFailureCode")
				.unset("deadLetteredAt")
				.unset("retentionReviewAt")
				.unset("leaseOwner")
				.unset("leaseExpiresAt");
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				UserMergedOutbox.class
		);
		return result.getModifiedCount() == 1;
	}

	private boolean updateLeased(String eventId, String leaseOwner, Update update) {
		Query query = Query.query(Criteria.where("_id")
				.is(requireText(eventId, "eventId"))
				.and("status").is(UserMergedOutboxStatus.IN_FLIGHT)
				.and("leaseOwner").is(requireText(leaseOwner, "leaseOwner")));
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				UserMergedOutbox.class
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
