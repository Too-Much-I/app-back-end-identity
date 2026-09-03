package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;

public final class OwnerEventPublishTransactionService {
	private static final Duration CORE_RETENTION_MARGIN = Duration.ofHours(24);
	private final OwnerEventDeliveryRepository deliveryRepository;
	private final OwnerEventConsumerStateRepository stateRepository;
	private final OwnerEventCoreRepository coreRepository;
	private final PhoneRejoinLineageRepository lineageRepository;

	public OwnerEventPublishTransactionService(
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventCoreRepository coreRepository,
			PhoneRejoinLineageRepository lineageRepository
	) {
		this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
		this.stateRepository = Objects.requireNonNull(stateRepository);
		this.coreRepository = Objects.requireNonNull(coreRepository);
		this.lineageRepository = Objects.requireNonNull(lineageRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public boolean complete(OwnerEventDelivery delivery, String leaseOwner,
			Instant publishedAt, Instant deliveryCleanupAt) {
		long expectedCursor = delivery.getConsumerSequence() - 1;
		if (!deliveryRepository.markPublished(delivery.getDeliveryId(), leaseOwner,
				publishedAt, deliveryCleanupAt)) return false;
		if (!stateRepository.advancePublished(delivery.getConsumer(), expectedCursor, publishedAt)) {
			throw new IllegalStateException("Owner event cursor advance failed");
		}
		coreRepository.findById(delivery.getEventId())
				.filter(core -> core.getEventType() == OwnerEventType.TRIAL_OWNER_REBIND_APPROVED)
				.ifPresent(core -> lineageRepository.findByClaimedEventId(core.getEventId())
						.ifPresent(lineage -> {
							lineage.markDeliveryPublished(deliveryCleanupAt);
							lineageRepository.save(lineage);
						}));
		if (deliveryRepository.countByEventIdAndStatusNot(
				delivery.getEventId(), OwnerEventDeliveryStatus.PUBLISHED) == 0) {
			List<OwnerEventDelivery> deliveries = deliveryRepository.findAllByEventId(delivery.getEventId());
			Instant latestCleanup = deliveries.stream().map(OwnerEventDelivery::getCleanupAt)
					.filter(Objects::nonNull).max(Comparator.naturalOrder())
					.orElse(deliveryCleanupAt);
			coreRepository.markAllPublished(delivery.getEventId(), publishedAt,
					latestCleanup.plus(CORE_RETENTION_MARGIN));
		}
		return true;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public boolean pause(OwnerEventDelivery delivery, String leaseOwner,
			OwnerEventFailureCode code, Instant now) {
		if (!deliveryRepository.releaseForCircuitPause(
				delivery.getDeliveryId(), leaseOwner, code, now)) return false;
		stateRepository.pause(delivery.getConsumer(), code, now);
		return true;
	}

	public boolean resume(OwnerEventConsumer consumer, Instant now) {
		return stateRepository.resume(consumer, now);
	}
}
