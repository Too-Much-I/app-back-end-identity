package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

public final class OwnerEventDeliveryRepositoryCustomImpl
		implements OwnerEventDeliveryRepositoryCustom {
	private final MongoOperations mongoOperations;

	public OwnerEventDeliveryRepositoryCustomImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
	}

	@Override
	public Optional<OwnerEventDelivery> claimExact(
			OwnerEventConsumer consumer, long sequence, String leaseOwner,
			Instant now, Instant leaseExpiresAt
	) {
		Criteria due = new Criteria().orOperator(
				new Criteria().andOperator(
						Criteria.where("status").is(OwnerEventDeliveryStatus.PENDING),
						Criteria.where("nextAttemptAt").lte(now)),
				new Criteria().andOperator(
						Criteria.where("status").is(OwnerEventDeliveryStatus.IN_FLIGHT),
						Criteria.where("leaseExpiresAt").lte(now)));
		Query query = Query.query(Criteria.where("consumer").is(Objects.requireNonNull(consumer))
				.and("consumerSequence").is(sequence).andOperator(due));
		Update update = new Update().set("status", OwnerEventDeliveryStatus.IN_FLIGHT)
				.set("leaseOwner", requireText(leaseOwner)).set("leaseExpiresAt", leaseExpiresAt)
				.inc("attemptCount", 1L);
		return Optional.ofNullable(mongoOperations.findAndModify(query, update,
				FindAndModifyOptions.options().returnNew(true), OwnerEventDelivery.class));
	}

	@Override
	public boolean markPublished(String deliveryId, String leaseOwner,
			Instant publishedAt, Instant cleanupAt) {
		return updateLeased(deliveryId, leaseOwner, new Update()
				.set("status", OwnerEventDeliveryStatus.PUBLISHED)
				.set("publishedAt", Objects.requireNonNull(publishedAt))
				.set("cleanupAt", Objects.requireNonNull(cleanupAt))
				.unset("leaseOwner").unset("leaseExpiresAt").unset("nextAttemptAt")
				.unset("lastFailureCode"));
	}

	@Override
	public boolean scheduleRetry(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant nextAttemptAt) {
		return updateLeased(deliveryId, leaseOwner, pending(failureCode, nextAttemptAt));
	}

	@Override
	public boolean releaseForCircuitPause(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant nextAttemptAt) {
		return updateLeased(deliveryId, leaseOwner, pending(failureCode, nextAttemptAt));
	}

	@Override
	public boolean markDeadLetter(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant deadLetteredAt, Instant reviewAt) {
		return updateLeased(deliveryId, leaseOwner, new Update()
				.set("status", OwnerEventDeliveryStatus.DEAD_LETTER)
				.set("lastFailureCode", Objects.requireNonNull(failureCode))
				.set("deadLetteredAt", Objects.requireNonNull(deadLetteredAt))
				.set("retentionReviewAt", Objects.requireNonNull(reviewAt))
				.unset("leaseOwner").unset("leaseExpiresAt").unset("nextAttemptAt")
				.unset("cleanupAt"));
	}

	@Override
	public boolean replayDeadLetter(String deliveryId, Instant replayAt) {
		Query query = Query.query(Criteria.where("_id").is(requireText(deliveryId))
				.and("status").is(OwnerEventDeliveryStatus.DEAD_LETTER));
		Update update = new Update().set("status", OwnerEventDeliveryStatus.PENDING)
				.set("attemptCount", 0).set("nextAttemptAt", Objects.requireNonNull(replayAt))
				.unset("lastFailureCode").unset("deadLetteredAt")
				.unset("retentionReviewAt").unset("leaseOwner").unset("leaseExpiresAt");
		return mongoOperations.updateFirst(query, update, OwnerEventDelivery.class)
				.getModifiedCount() == 1;
	}

	private Update pending(OwnerEventFailureCode failureCode, Instant nextAttemptAt) {
		return new Update().set("status", OwnerEventDeliveryStatus.PENDING)
				.set("lastFailureCode", Objects.requireNonNull(failureCode))
				.set("nextAttemptAt", Objects.requireNonNull(nextAttemptAt))
				.unset("leaseOwner").unset("leaseExpiresAt");
	}

	private boolean updateLeased(String id, String owner, Update update) {
		Query query = Query.query(Criteria.where("_id").is(requireText(id))
				.and("status").is(OwnerEventDeliveryStatus.IN_FLIGHT)
				.and("leaseOwner").is(requireText(owner)));
		return mongoOperations.updateFirst(query, update, OwnerEventDelivery.class)
				.getModifiedCount() == 1;
	}

	private static String requireText(String value) {
		String required = Objects.requireNonNull(value);
		if (required.isBlank()) throw new IllegalArgumentException("value must not be blank");
		return required;
	}
}
