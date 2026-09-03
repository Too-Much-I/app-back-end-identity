package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;

public final class OwnerEventCaptureService {
	private final OwnerEventCoreRepository coreRepository;
	private final OwnerEventDeliveryRepository deliveryRepository;
	private final OwnerEventConsumerStateRepository stateRepository;

	public OwnerEventCaptureService(
			OwnerEventCoreRepository coreRepository,
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository
	) {
		this.coreRepository = Objects.requireNonNull(coreRepository);
		this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
		this.stateRepository = Objects.requireNonNull(stateRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public OwnerEventCore captureUserMerged(
			String sourceUserId, String targetUserId, Instant occurredAt
	) {
		return persist(OwnerEventCore.userMerged(sourceUserId, targetUserId, occurredAt));
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public OwnerEventCore captureTrialOwnerRebind(
			String sourceUserId, String targetUserId, String consumerScopeId,
			long sourceBindingRevision, long targetBindingRevision,
			String lineageId, Instant occurredAt
	) {
		return persist(OwnerEventCore.trialOwnerRebindApproved(
				sourceUserId, targetUserId, consumerScopeId, sourceBindingRevision,
				targetBindingRevision, lineageId, occurredAt));
	}

	private OwnerEventCore persist(OwnerEventCore event) {
		List<OwnerEventDelivery> deliveries = new ArrayList<>();
		for (OwnerEventConsumer consumer : event.getRequiredConsumers().stream().sorted().toList()) {
			long sequence = stateRepository.allocateNext(consumer, event.getOccurredAt());
			deliveries.add(OwnerEventDelivery.create(
					event.getEventId(), consumer, sequence, event.getOccurredAt()));
		}
		coreRepository.save(event);
		deliveryRepository.saveAll(deliveries);
		return event;
	}
}
