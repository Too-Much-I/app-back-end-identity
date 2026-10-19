package web.tosunsaeng.identity.domain.auth.federation.infrastructure;

import org.springframework.scheduling.annotation.Scheduled;

import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseCleanupOutcome;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseEnrollmentCleanupWorker;

public final class FirebaseAbandonedCleanupScheduler {

	private final AbandonedFirebaseEnrollmentCleanupWorker worker;
	private final int maxBatchSize;

	public FirebaseAbandonedCleanupScheduler(
			AbandonedFirebaseEnrollmentCleanupWorker worker,
			int maxBatchSize
	) {
		this.worker = worker;
		if (maxBatchSize < 1 || maxBatchSize > 100) {
			throw new IllegalArgumentException("maxBatchSize must be between 1 and 100");
		}
		this.maxBatchSize = maxBatchSize;
	}

	@Scheduled(fixedDelayString = "${app.firebase-abandoned-cleanup.fixed-delay:PT5S}")
	public void processBatch() {
		for (int processed = 0; processed < maxBatchSize; processed++) {
			if (worker.processNext() == AbandonedFirebaseCleanupOutcome.NONE) return;
		}
	}
}
