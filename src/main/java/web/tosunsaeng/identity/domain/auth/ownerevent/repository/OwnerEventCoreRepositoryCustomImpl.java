package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;
import java.util.Objects;

import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;

public final class OwnerEventCoreRepositoryCustomImpl implements OwnerEventCoreRepositoryCustom {
	private final MongoOperations mongoOperations;

	public OwnerEventCoreRepositoryCustomImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(mongoOperations);
	}

	@Override
	public boolean markAllPublished(String eventId, Instant allPublishedAt, Instant cleanupAt) {
		Query query = Query.query(Criteria.where("_id").is(Objects.requireNonNull(eventId))
				.and("allPublishedAt").is(null));
		Update update = new Update().set("allPublishedAt", Objects.requireNonNull(allPublishedAt))
				.set("cleanupAt", Objects.requireNonNull(cleanupAt));
		return mongoOperations.updateFirst(query, update, OwnerEventCore.class).getModifiedCount() == 1;
	}
}
