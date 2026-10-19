package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.time.Clock;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingPublisher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;

public final class PhoneEligibilityBindingPublisherScheduler {

	private final PhoneEligibilityBindingPublisher publisher;
	private final PhoneEligibilityBindingOutboxRepository outboxRepository;
	private final int maxBatchSize;
	private final Clock clock;

	public PhoneEligibilityBindingPublisherScheduler(
			PhoneEligibilityBindingPublisher publisher,
			PhoneEligibilityBindingOutboxRepository outboxRepository,
			int maxBatchSize,
			Clock clock
	) {
		this.publisher = publisher;
		this.outboxRepository = outboxRepository;
		this.maxBatchSize = maxBatchSize;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${app.phone-eligibility-publisher.fixed-delay:PT5S}")
	public void publishAndCleanup() {
		for (int index = 0; index < maxBatchSize; index++) {
			PhoneEligibilityBindingPublisher.Outcome outcome = publisher.publishNext();
			if (outcome == PhoneEligibilityBindingPublisher.Outcome.NONE
					|| outcome == PhoneEligibilityBindingPublisher.Outcome.SCOPE_PAUSED) break;
		}
		outboxRepository.deleteByStatusAndCleanupAtLessThanEqual(
				PhoneEligibilityBindingOutboxStatus.PUBLISHED, clock.instant());
	}
}
