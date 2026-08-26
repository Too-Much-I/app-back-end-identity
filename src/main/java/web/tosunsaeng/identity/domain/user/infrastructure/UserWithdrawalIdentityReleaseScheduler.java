package web.tosunsaeng.identity.domain.user.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.user.application.IdentityReleaseOutcome;
import web.tosunsaeng.identity.domain.user.application.UserWithdrawalIdentityReleaseWorker;

public final class UserWithdrawalIdentityReleaseScheduler {

	private final UserWithdrawalIdentityReleaseWorker worker;
	private final int maxBatchSize;

	public UserWithdrawalIdentityReleaseScheduler(
			UserWithdrawalIdentityReleaseWorker worker,
			int maxBatchSize
	) {
		this.worker = worker;
		if (maxBatchSize < 1 || maxBatchSize > 100) {
			throw new IllegalArgumentException("maxBatchSize must be between 1 and 100");
		}
		this.maxBatchSize = maxBatchSize;
	}

	@Scheduled(
			fixedDelayString = "${app.firebase-withdrawal-identity-release.fixed-delay:PT5S}"
	)
	public void processBatch() {
		for (int processed = 0; processed < maxBatchSize; processed++) {
			IdentityReleaseOutcome outcome = worker.processNext();
			if (outcome == IdentityReleaseOutcome.NONE
					|| outcome == IdentityReleaseOutcome.TRANSIENT_FAILURE
					|| outcome == IdentityReleaseOutcome.CONCURRENT_CHANGE) {
				return;
			}
		}
	}
}
