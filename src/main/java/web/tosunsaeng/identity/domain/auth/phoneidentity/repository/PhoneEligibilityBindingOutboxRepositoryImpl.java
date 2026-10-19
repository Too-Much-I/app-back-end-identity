package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

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

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;

class MongoPhoneEligibilityBindingOutboxOperations
		implements PhoneEligibilityBindingOutboxRepositoryCustom {

	private final MongoOperations mongoOperations;

	MongoPhoneEligibilityBindingOutboxOperations(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(
				mongoOperations,
				"mongoOperations must not be null"
		);
	}

	@Override
	public Optional<PhoneEligibilityBindingOutbox> claimNext(
			String consumerScopeId,
			String leaseOwner,
			Instant claimedAt,
			Instant leaseExpiresAt
	) {
		String requiredScope = requireText(consumerScopeId, "consumerScopeId");
		String requiredOwner = requireText(leaseOwner, "leaseOwner");
		Instant requiredClaimedAt = Objects.requireNonNull(claimedAt, "claimedAt must not be null");
		Instant requiredLeaseExpiresAt = Objects.requireNonNull(
				leaseExpiresAt,
				"leaseExpiresAt must not be null"
		);
		if (!requiredLeaseExpiresAt.isAfter(requiredClaimedAt)) {
			throw new IllegalArgumentException("leaseExpiresAt must be after claimedAt");
		}

		Criteria pendingDue = new Criteria().andOperator(
				Criteria.where("status").is(PhoneEligibilityBindingOutboxStatus.PENDING),
				Criteria.where("nextAttemptAt").lte(requiredClaimedAt)
		);
		Criteria expiredLease = new Criteria().andOperator(
				Criteria.where("status").is(PhoneEligibilityBindingOutboxStatus.IN_FLIGHT),
				Criteria.where("leaseExpiresAt").lte(requiredClaimedAt)
		);
		Query query = Query.query(new Criteria().andOperator(
				Criteria.where("consumerScopeId").is(requiredScope),
				new Criteria().orOperator(pendingDue, expiredLease)
		)).with(Sort.by(Sort.Direction.ASC, "occurredAt", "eventId"));
		Update update = new Update()
				.set("status", PhoneEligibilityBindingOutboxStatus.IN_FLIGHT)
				.set("leaseOwner", requiredOwner)
				.set("leaseExpiresAt", requiredLeaseExpiresAt)
				.inc("attemptCount", 1L);
		PhoneEligibilityBindingOutbox claimed = mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				PhoneEligibilityBindingOutbox.class
		);
		return Optional.ofNullable(claimed);
	}

	@Override
	public boolean markPublished(
			String eventId,
			String leaseOwner,
			Instant publishedAt,
			Instant cleanupAt
	) {
		Update update = new Update()
				.set("status", PhoneEligibilityBindingOutboxStatus.PUBLISHED)
				.set("publishedAt", Objects.requireNonNull(publishedAt, "publishedAt must not be null"))
				.set("cleanupAt", Objects.requireNonNull(cleanupAt, "cleanupAt must not be null"))
				.unset("leaseOwner")
				.unset("leaseExpiresAt")
				.unset("nextAttemptAt")
				.unset("lastFailureCode");
		return updateLeased(eventId, leaseOwner, update);
	}

	@Override
	public boolean scheduleRetry(
			String eventId,
			String leaseOwner,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant nextAttemptAt
	) {
		Update update = new Update()
				.set("status", PhoneEligibilityBindingOutboxStatus.PENDING)
				.set("lastFailureCode", Objects.requireNonNull(
						failureCode,
						"failureCode must not be null"
				))
				.set("nextAttemptAt", Objects.requireNonNull(
						nextAttemptAt,
						"nextAttemptAt must not be null"
				))
				.unset("leaseOwner")
				.unset("leaseExpiresAt");
		return updateLeased(eventId, leaseOwner, update);
	}

	@Override
	public boolean markDeadLetter(
			String eventId,
			String leaseOwner,
			PhoneEligibilityBindingFailureCode failureCode,
			Instant deadLetteredAt,
			Instant retentionReviewAt
	) {
		Update update = new Update()
				.set("status", PhoneEligibilityBindingOutboxStatus.DEAD_LETTER)
				.set("lastFailureCode", Objects.requireNonNull(
						failureCode,
						"failureCode must not be null"
				))
				.set("deadLetteredAt", Objects.requireNonNull(
						deadLetteredAt,
						"deadLetteredAt must not be null"
				))
				.set("retentionReviewAt", Objects.requireNonNull(
						retentionReviewAt,
						"retentionReviewAt must not be null"
				))
				.unset("leaseOwner")
				.unset("leaseExpiresAt")
				.unset("nextAttemptAt");
		return updateLeased(eventId, leaseOwner, update);
	}

	@Override
	public boolean replayDeadLetter(String eventId, Instant replayAt) {
		Query query = Query.query(Criteria.where("_id")
				.is(requireText(eventId, "eventId"))
				.and("status").is(PhoneEligibilityBindingOutboxStatus.DEAD_LETTER));
		Update update = new Update()
				.set("status", PhoneEligibilityBindingOutboxStatus.PENDING)
				.set("attemptCount", 0)
				.set("nextAttemptAt", Objects.requireNonNull(replayAt, "replayAt must not be null"))
				.unset("lastFailureCode")
				.unset("deadLetteredAt")
				.unset("retentionReviewAt")
				.unset("leaseOwner")
				.unset("leaseExpiresAt");
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				PhoneEligibilityBindingOutbox.class
		);
		return result.getModifiedCount() == 1;
	}

	private boolean updateLeased(String eventId, String leaseOwner, Update update) {
		Query query = Query.query(Criteria.where("_id")
				.is(requireText(eventId, "eventId"))
				.and("status").is(PhoneEligibilityBindingOutboxStatus.IN_FLIGHT)
				.and("leaseOwner").is(requireText(leaseOwner, "leaseOwner")));
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				PhoneEligibilityBindingOutbox.class
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
