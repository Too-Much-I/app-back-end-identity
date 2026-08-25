package web.tosunsaeng.identity.domain.user.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.user.application.UserWithdrawalExternalCleanupWorker;
import web.tosunsaeng.identity.domain.user.application.WithdrawalCleanupOutcome;

public final class UserWithdrawalExternalCleanupScheduler {

	private final UserWithdrawalExternalCleanupWorker worker;
	private final int maxBatchSize;

	public UserWithdrawalExternalCleanupScheduler(
			UserWithdrawalExternalCleanupWorker worker,
			int maxBatchSize
	) {
		this.worker = worker;
		if (maxBatchSize < 1 || maxBatchSize > 100) {
			throw new IllegalArgumentException("maxBatchSize must be between 1 and 100");
		}
		this.maxBatchSize = maxBatchSize;
	}

	@Scheduled(fixedDelayString = "${app.firebase-withdrawal-cleanup.fixed-delay:PT5S}")
	public void processBatch() {
		int processed = 0;
		while (processed < maxBatchSize) {
			WithdrawalCleanupOutcome handoff = worker.handoffNextCompleted();
			if (handoff != WithdrawalCleanupOutcome.HANDED_OFF) break;
			processed++;
		}
		while (processed < maxBatchSize) {
			WithdrawalCleanupOutcome outcome = worker.processNext();
			if (outcome == WithdrawalCleanupOutcome.NONE) break;
			processed++;
		}
	}
}
