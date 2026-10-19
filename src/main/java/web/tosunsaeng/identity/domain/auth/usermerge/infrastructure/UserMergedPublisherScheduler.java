package web.tosunsaeng.identity.domain.auth.usermerge.infrastructure;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedOutboxStatus;
import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedPublisher;
import web.tosunsaeng.identity.domain.auth.usermerge.repository.UserMergedOutboxRepository;

public final class UserMergedPublisherScheduler {

	private final UserMergedPublisher publisher;
	private final UserMergedOutboxRepository outboxRepository;
	private final int maxBatchSize;
	private final Clock clock;

	public UserMergedPublisherScheduler(
			UserMergedPublisher publisher,
			UserMergedOutboxRepository outboxRepository,
			int maxBatchSize,
			Clock clock
	) {
		this.publisher = publisher;
		this.outboxRepository = outboxRepository;
		this.maxBatchSize = maxBatchSize;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.user-merged-publisher.fixed-delay:PT5S}")
	public void publishAndCleanup() {
		for (int index = 0; index < maxBatchSize; index++) {
			if (publisher.publishNext() == UserMergedPublisher.Outcome.NONE) {
				break;
			}
		}
		outboxRepository.deleteByStatusAndCleanupAtLessThanEqual(
				UserMergedOutboxStatus.PUBLISHED,
				clock.instant()
		);
	}
}
