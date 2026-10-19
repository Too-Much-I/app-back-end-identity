package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

public interface OwnerEventDeliveryRepositoryCustom {
	Optional<OwnerEventDelivery> claimExact(OwnerEventConsumer consumer, long sequence,
			String leaseOwner, Instant now, Instant leaseExpiresAt);
	boolean markPublished(String deliveryId, String leaseOwner, Instant publishedAt, Instant cleanupAt);
	boolean scheduleRetry(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant nextAttemptAt);
	boolean releaseForCircuitPause(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant nextAttemptAt);
	boolean markDeadLetter(String deliveryId, String leaseOwner,
			OwnerEventFailureCode failureCode, Instant deadLetteredAt, Instant reviewAt);
	boolean replayDeadLetter(String deliveryId, Instant replayAt);
}
