package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;

public interface OwnerEventConsumerStateRepository extends
		MongoRepository<OwnerEventConsumerState, OwnerEventConsumer>,
		OwnerEventConsumerStateRepositoryCustom {
}
