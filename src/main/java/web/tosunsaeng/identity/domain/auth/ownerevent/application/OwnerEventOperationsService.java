package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Clock;
import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;

public final class OwnerEventOperationsService {
	private final OwnerEventDeliveryRepository deliveryRepository;
	private final OwnerEventConsumerStateRepository stateRepository;
	private final OwnerEventPublishTransactionService transactionService;
	private final Clock clock;

	public OwnerEventOperationsService(
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventPublishTransactionService transactionService,
			Clock clock
	) {
		this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
		this.stateRepository = Objects.requireNonNull(stateRepository);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.clock = Objects.requireNonNull(clock);
	}

	public boolean replayDeadLetter(String deliveryId) {
		OwnerEventDelivery delivery = deliveryRepository.findById(
				Objects.requireNonNull(deliveryId)).orElse(null);
		if (delivery == null || delivery.getStatus() != OwnerEventDeliveryStatus.DEAD_LETTER) {
			return false;
		}
		OwnerEventConsumerState state = stateRepository.findById(delivery.getConsumer())
				.orElseThrow(() -> new IllegalStateException("Owner event consumer state is missing"));
		if (delivery.getConsumerSequence() != state.getLastPublishedSequence() + 1) {
			throw new IllegalStateException("Only the exact head delivery can be replayed");
		}
		return deliveryRepository.replayDeadLetter(deliveryId, clock.instant());
	}

	public boolean resumeCircuit(OwnerEventConsumer consumer) {
		return transactionService.resume(Objects.requireNonNull(consumer), clock.instant());
	}
}
