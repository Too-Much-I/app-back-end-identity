package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnOutboxStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.domain.user.withdrawalevent.application.UserWithdrawnPublisher;

public final class UserWithdrawnPublisherScheduler {

	private final UserWithdrawnPublisher publisher;
	private final UserWithdrawnOutboxRepository repository;
	private final int maxBatchSize;
	private final Clock clock;

	public UserWithdrawnPublisherScheduler(
			UserWithdrawnPublisher publisher,
			UserWithdrawnOutboxRepository repository,
			int maxBatchSize,
			Clock clock
	) {
		this.publisher = publisher;
		this.repository = repository;
		this.maxBatchSize = maxBatchSize;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.user-withdrawn-publisher.fixed-delay:PT5S}")
	public void publishAndCleanup() {
		for (int index = 0; index < maxBatchSize; index++) {
			if (publisher.publishNext() == UserWithdrawnPublisher.Outcome.NONE) {
				break;
			}
		}
		repository.deleteByStatusAndCleanupAtLessThanEqual(
				UserWithdrawnOutboxStatus.PUBLISHED,
				clock.instant()
		);
	}
}
