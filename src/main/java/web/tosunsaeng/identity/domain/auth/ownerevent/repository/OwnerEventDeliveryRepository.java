package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;

public interface OwnerEventDeliveryRepository
		extends MongoRepository<OwnerEventDelivery, String>, OwnerEventDeliveryRepositoryCustom {

	Optional<OwnerEventDelivery> findByConsumerAndConsumerSequence(
			OwnerEventConsumer consumer, long consumerSequence);

	List<OwnerEventDelivery> findAllByEventId(String eventId);

	long countByEventIdAndStatusNot(String eventId, OwnerEventDeliveryStatus status);

	long deleteByStatusAndCleanupAtLessThanEqual(OwnerEventDeliveryStatus status, Instant cleanupAt);
}
