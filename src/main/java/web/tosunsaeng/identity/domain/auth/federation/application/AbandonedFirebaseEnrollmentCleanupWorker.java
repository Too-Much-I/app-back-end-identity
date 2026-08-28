package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;

public final class AbandonedFirebaseEnrollmentCleanupWorker {

	private static final Logger log = LoggerFactory.getLogger(
			AbandonedFirebaseEnrollmentCleanupWorker.class
	);

	private final AbandonedFirebaseEnrollmentCleanupRepository repository;
	private final AbandonedFirebaseEnrollmentTargetGuard targetGuard;
	private final AbandonedFirebaseUserCleanupPort cleanupPort;
	private final AbandonedFirebaseCleanupRetryPolicy retryPolicy;
	private final AbandonedFirebaseCleanupTerminalTransactionService terminalService;
	private final MeterRegistry meterRegistry;
	private final Clock clock;
	private final Duration leaseDuration;
	private final int maxAttempts;

	public AbandonedFirebaseEnrollmentCleanupWorker(
			AbandonedFirebaseEnrollmentCleanupRepository repository,
			AbandonedFirebaseEnrollmentTargetGuard targetGuard,
			AbandonedFirebaseUserCleanupPort cleanupPort,
			AbandonedFirebaseCleanupRetryPolicy retryPolicy,
			AbandonedFirebaseCleanupTerminalTransactionService terminalService,
			MeterRegistry meterRegistry,
			Clock clock,
			Duration leaseDuration,
			int maxAttempts
	) {
		this.repository = Objects.requireNonNull(repository);
		this.targetGuard = Objects.requireNonNull(targetGuard);
		this.cleanupPort = Objects.requireNonNull(cleanupPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.terminalService = Objects.requireNonNull(terminalService);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
		this.leaseDuration = requirePositive(leaseDuration, "leaseDuration");
		if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
		this.maxAttempts = maxAttempts;
	}

	public AbandonedFirebaseCleanupOutcome processNext() {
		Instant claimedAt = clock.instant();
		Optional<AbandonedFirebaseEnrollmentCleanup> claimed = repository.claimNext(
				UUID.randomUUID().toString(), claimedAt, claimedAt.plus(leaseDuration)
		);
		if (claimed.isEmpty()) return record(AbandonedFirebaseCleanupOutcome.NONE, null, 0);
		AbandonedFirebaseEnrollmentCleanup cleanup = claimed.orElseThrow();
		long version = requireVersion(cleanup);

		var localGuard = targetGuard.verifyLocal(cleanup, clock.instant());
		if (localGuard.leaseLost()) return recordLeaseLost(cleanup);
		if (!localGuard.allowed()) return reconcile(cleanup, version, localGuard.failureCode());

		try {
			AbandonedFirebaseAccountSnapshot snapshot = cleanupPort.inspect(
					cleanup.getFirebaseProjectId(), cleanup.getFirebaseUid()
			);
			var externalGuard = targetGuard.verifyExternalOwners(snapshot);
			if (!externalGuard.allowed()) {
				return reconcile(cleanup, version, externalGuard.failureCode());
			}
			version = renew(cleanup, version);
			if (version < 0) return recordLeaseLost(cleanup);

			cleanupPort.disable(cleanup.getFirebaseProjectId(), cleanup.getFirebaseUid());
			version = renew(cleanup, version);
			if (version < 0) return recordLeaseLost(cleanup);

			cleanupPort.revokeRefreshTokens(
					cleanup.getFirebaseProjectId(), cleanup.getFirebaseUid()
			);
			version = renew(cleanup, version);
			if (version < 0) return recordLeaseLost(cleanup);

			cleanupPort.satisfyProviderDeletionObligations(snapshot);
			version = renew(cleanup, version);
			if (version < 0) return recordLeaseLost(cleanup);

			try {
				cleanupPort.delete(cleanup.getFirebaseProjectId(), cleanup.getFirebaseUid());
			} catch (AbandonedFirebaseCleanupException exception) {
				if (shouldConfirm(exception.failureCode())) {
					return confirmAbsent(cleanup, version);
				}
				throw exception;
			}
			return confirmAbsent(cleanup, version);
		} catch (AbandonedFirebaseCleanupException exception) {
			if (exception.failureCode() == AbandonedFirebaseEnrollmentFailureCode.NOT_FOUND) {
				return complete(cleanup, version);
			}
			return handleFailure(cleanup, version, exception.failureCode());
		}
	}

	private AbandonedFirebaseCleanupOutcome confirmAbsent(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long version
	) {
		try {
			if (cleanupPort.checkPresence(
					cleanup.getFirebaseProjectId(), cleanup.getFirebaseUid()
			) == AbandonedFirebaseAccountPresence.ABSENT) {
				return complete(cleanup, version);
			}
			return retryOrReconcile(
					cleanup, version, AbandonedFirebaseEnrollmentFailureCode.DELETE_NOT_CONFIRMED
			);
		} catch (AbandonedFirebaseCleanupException exception) {
			return handleFailure(cleanup, version, exception.failureCode());
		}
	}

	private AbandonedFirebaseCleanupOutcome handleFailure(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long version,
			AbandonedFirebaseEnrollmentFailureCode code
	) {
		if (code == AbandonedFirebaseEnrollmentFailureCode.NOT_FOUND) {
			return complete(cleanup, version);
		}
		return code.isRetryable()
				? retryOrReconcile(cleanup, version, code)
				: reconcile(cleanup, version, code);
	}

	private AbandonedFirebaseCleanupOutcome retryOrReconcile(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long version,
			AbandonedFirebaseEnrollmentFailureCode code
	) {
		if (cleanup.getAttemptCount() >= maxAttempts) {
			return reconcile(cleanup, version, code);
		}
		Instant now = clock.instant();
		boolean updated = repository.scheduleRetry(
				cleanup.getCleanupId(), cleanup.getLeaseOwner(), cleanup.getGeneration(), version,
				code, now.plus(retryPolicy.delay(cleanup.getAttemptCount())), now
		);
		return record(
				updated ? AbandonedFirebaseCleanupOutcome.RETRY_SCHEDULED
						: AbandonedFirebaseCleanupOutcome.LEASE_LOST,
				code, cleanup.getAttemptCount()
		);
	}

	private AbandonedFirebaseCleanupOutcome reconcile(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long version,
			AbandonedFirebaseEnrollmentFailureCode code
	) {
		boolean updated = repository.markReconciliationRequired(
				cleanup.getCleanupId(), cleanup.getLeaseOwner(), cleanup.getGeneration(), version,
				code, clock.instant()
		);
		return record(
				updated ? AbandonedFirebaseCleanupOutcome.RECONCILIATION_REQUIRED
						: AbandonedFirebaseCleanupOutcome.LEASE_LOST,
				code, cleanup.getAttemptCount()
		);
	}

	private AbandonedFirebaseCleanupOutcome complete(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long version
	) {
		boolean updated = terminalService.markCleaned(cleanup, version, clock.instant());
		return record(
				updated ? AbandonedFirebaseCleanupOutcome.CLEANED
						: AbandonedFirebaseCleanupOutcome.LEASE_LOST,
				null, cleanup.getAttemptCount()
		);
	}

	private long renew(AbandonedFirebaseEnrollmentCleanup cleanup, long version) {
		Instant now = clock.instant();
		boolean renewed = repository.renewLease(
				cleanup.getCleanupId(), cleanup.getLeaseOwner(), cleanup.getGeneration(), version,
				now.plus(leaseDuration), now
		);
		return renewed ? version + 1 : -1;
	}

	private AbandonedFirebaseCleanupOutcome recordLeaseLost(
			AbandonedFirebaseEnrollmentCleanup cleanup
	) {
		return record(AbandonedFirebaseCleanupOutcome.LEASE_LOST, null, cleanup.getAttemptCount());
	}

	private AbandonedFirebaseCleanupOutcome record(
			AbandonedFirebaseCleanupOutcome outcome,
			AbandonedFirebaseEnrollmentFailureCode failure,
			int attemptCount
	) {
		String safeCode = failure == null ? "NONE" : failure.name();
		meterRegistry.counter(
				"identity.firebase.enrollment.cleanup.outcome",
				"outcome", outcome.name(),
				"failureCode", safeCode
		).increment();
		if (outcome != AbandonedFirebaseCleanupOutcome.NONE) {
			log.atInfo()
					.addKeyValue("event", "firebase.enrollment.cleanup")
					.addKeyValue("outcome", outcome.name())
					.addKeyValue("failureCode", safeCode)
					.addKeyValue("attemptCount", attemptCount)
					.log("Abandoned Firebase enrollment cleanup outcome");
		}
		return outcome;
	}

	private static boolean shouldConfirm(AbandonedFirebaseEnrollmentFailureCode code) {
		return code == AbandonedFirebaseEnrollmentFailureCode.TIMEOUT
				|| code == AbandonedFirebaseEnrollmentFailureCode.UNAVAILABLE
				|| code == AbandonedFirebaseEnrollmentFailureCode.RESULT_UNKNOWN;
	}

	private static long requireVersion(AbandonedFirebaseEnrollmentCleanup cleanup) {
		Long version = cleanup.getVersion();
		if (version == null || version < 0) {
			throw new IllegalStateException("Claimed cleanup must have a version.");
		}
		return version;
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value);
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}
}
