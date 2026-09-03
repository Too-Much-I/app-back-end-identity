package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

public interface OwnerEventConsumerStateRepositoryCustom {
	long allocateNext(OwnerEventConsumer consumer, Instant now);
	boolean advancePublished(OwnerEventConsumer consumer, long expectedLastPublished, Instant now);
	boolean pause(OwnerEventConsumer consumer, OwnerEventFailureCode code, Instant now);
	boolean resume(OwnerEventConsumer consumer, Instant now);
}
