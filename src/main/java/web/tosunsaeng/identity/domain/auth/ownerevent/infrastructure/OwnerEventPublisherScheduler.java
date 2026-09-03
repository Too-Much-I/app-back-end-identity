package web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventPublisher;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;

public final class OwnerEventPublisherScheduler {
	private final OwnerEventPublisher publisher;
	private final OwnerEventDeliveryRepository deliveryRepository;
	private final OwnerEventCoreRepository coreRepository;
	private final int maxBatchSize;
	private final Clock clock;

	public OwnerEventPublisherScheduler(
			OwnerEventPublisher publisher, OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventCoreRepository coreRepository, int maxBatchSize, Clock clock
	) {
		this.publisher = publisher;
		this.deliveryRepository = deliveryRepository;
		this.coreRepository = coreRepository;
		this.maxBatchSize = maxBatchSize;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.owner-event.fixed-delay:PT5S}")
	public void publishAndCleanup() {
		for (int index = 0; index < maxBatchSize; index++) {
			OwnerEventPublisher.Outcome outcome = publisher.publishNext();
			if (outcome == OwnerEventPublisher.Outcome.NONE
					|| outcome == OwnerEventPublisher.Outcome.CIRCUIT_PAUSED
					|| outcome == OwnerEventPublisher.Outcome.BLOCKED_BY_DISABLED_CHANNEL) break;
		}
		deliveryRepository.deleteByStatusAndCleanupAtLessThanEqual(
				OwnerEventDeliveryStatus.PUBLISHED, clock.instant());
		coreRepository.deleteByCleanupAtLessThanEqual(clock.instant());
	}
}
