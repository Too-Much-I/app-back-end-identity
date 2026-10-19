package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventCircuitStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

public final class OwnerEventConsumerStateRepositoryCustomImpl
		implements OwnerEventConsumerStateRepositoryCustom {
	private final MongoOperations mongoOperations;

	public OwnerEventConsumerStateRepositoryCustomImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
	}

	@Override
	public long allocateNext(OwnerEventConsumer consumer, Instant now) {
		Query query = Query.query(Criteria.where("_id").is(Objects.requireNonNull(consumer)));
		Update update = new Update().inc("lastAllocatedSequence", 1L)
				.inc("version", 1L)
				.set("updatedAt", Objects.requireNonNull(now))
				.setOnInsert("lastPublishedSequence", 0L)
				.setOnInsert("circuitStatus", OwnerEventCircuitStatus.ACTIVE);
		OwnerEventConsumerState state = mongoOperations.findAndModify(query, update,
				FindAndModifyOptions.options().upsert(true).returnNew(true),
				OwnerEventConsumerState.class);
		if (state == null || state.getLastAllocatedSequence() < 1) {
			throw new IllegalStateException("Owner event sequence allocation failed");
		}
		return state.getLastAllocatedSequence();
	}

	@Override
	public boolean advancePublished(OwnerEventConsumer consumer,
			long expectedLastPublished, Instant now) {
		Query query = Query.query(Criteria.where("_id").is(consumer)
				.and("lastPublishedSequence").is(expectedLastPublished)
				.and("circuitStatus").is(OwnerEventCircuitStatus.ACTIVE));
		Update update = new Update().set("lastPublishedSequence", expectedLastPublished + 1)
				.set("updatedAt", Objects.requireNonNull(now)).inc("version", 1L);
		return mongoOperations.updateFirst(query, update, OwnerEventConsumerState.class)
				.getModifiedCount() == 1;
	}

	@Override
	public boolean pause(OwnerEventConsumer consumer, OwnerEventFailureCode code, Instant now) {
		Query query = Query.query(Criteria.where("_id").is(consumer)
				.and("circuitStatus").is(OwnerEventCircuitStatus.ACTIVE));
		Update update = new Update().set("circuitStatus", OwnerEventCircuitStatus.PAUSED)
				.set("pauseFailureCode", Objects.requireNonNull(code))
				.set("pausedAt", Objects.requireNonNull(now)).set("updatedAt", now)
				.inc("version", 1L);
		return mongoOperations.updateFirst(query, update, OwnerEventConsumerState.class)
				.getModifiedCount() == 1;
	}

	@Override
	public boolean resume(OwnerEventConsumer consumer, Instant now) {
		Query query = Query.query(Criteria.where("_id").is(consumer)
				.and("circuitStatus").is(OwnerEventCircuitStatus.PAUSED));
		Update update = new Update().set("circuitStatus", OwnerEventCircuitStatus.ACTIVE)
				.set("updatedAt", Objects.requireNonNull(now))
				.unset("pauseFailureCode").unset("pausedAt").inc("version", 1L);
		return mongoOperations.updateFirst(query, update, OwnerEventConsumerState.class)
				.getModifiedCount() == 1;
	}
}
