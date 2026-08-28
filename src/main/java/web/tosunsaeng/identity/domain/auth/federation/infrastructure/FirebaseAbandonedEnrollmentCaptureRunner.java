package web.tosunsaeng.identity.domain.auth.federation.infrastructure;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;

import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseCleanupException;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseEnrollmentTargetGuard;
import web.tosunsaeng.identity.domain.auth.federation.application.AbandonedFirebaseUserCleanupPort;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAbandonedEnrollmentCaptureTransactionService;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

public final class FirebaseAbandonedEnrollmentCaptureRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(
			FirebaseAbandonedEnrollmentCaptureRunner.class
	);
	private final FirebaseEnrollmentAttemptRepository repository;
	private final FirebaseAbandonedEnrollmentCaptureTransactionService transactionService;
	private final FirebaseAbandonedCleanupProperties properties;
	private final AbandonedFirebaseUserCleanupPort cleanupPort;
	private final AbandonedFirebaseEnrollmentTargetGuard targetGuard;
	private final Clock clock;

	public FirebaseAbandonedEnrollmentCaptureRunner(
			FirebaseEnrollmentAttemptRepository repository,
			FirebaseAbandonedEnrollmentCaptureTransactionService transactionService,
			AbandonedFirebaseUserCleanupPort cleanupPort,
			AbandonedFirebaseEnrollmentTargetGuard targetGuard,
			FirebaseAbandonedCleanupProperties properties,
			Clock clock
	) {
		this.repository = repository;
		this.transactionService = transactionService;
		this.cleanupPort = cleanupPort;
		this.targetGuard = targetGuard;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public void run(ApplicationArguments args) {
		properties.validate();
		var now = clock.instant();
		var candidates = repository.findLegacyCaptureCandidates(
				properties.getCaptureLowerBound(), properties.getCaptureUpperBound(), now,
				properties.getCaptureMaxBatchSize()
		);
		int eligible = 0;
		for (var candidate : candidates) {
			try {
				var snapshot = cleanupPort.inspect(
						candidate.getFirebaseProjectId(), candidate.getFirebaseUid()
				);
				if (!targetGuard.verifyExternalOwners(snapshot).allowed()) continue;
				if (transactionService.captureIfEligible(
						candidate, properties.getGrace(), now, properties.isCaptureDryRun()
				)) eligible++;
			} catch (DuplicateKeyException | AbandonedFirebaseCleanupException exception) {
				// 다른 instance가 동일 target을 capture한 경우 멱등 skip한다.
			}
		}
		log.atInfo()
				.addKeyValue("event", "firebase.enrollment.capture")
				.addKeyValue("dryRun", properties.isCaptureDryRun())
				.addKeyValue("scanned", candidates.size())
				.addKeyValue("eligible", eligible)
				.log("Bounded Firebase enrollment capture completed");
	}
}
