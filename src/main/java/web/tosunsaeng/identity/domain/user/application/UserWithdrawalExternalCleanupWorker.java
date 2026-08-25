package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class UserWithdrawalExternalCleanupWorker {

	private static final Logger log = LoggerFactory.getLogger(
			UserWithdrawalExternalCleanupWorker.class
	);

	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final WithdrawalCleanupTargetGuard targetGuard;
	private final FirebaseWithdrawalCleanupPort cleanupPort;
	private final WithdrawalCleanupRetryPolicy retryPolicy;
	private final MeterRegistry meterRegistry;
	private final Clock clock;
	private final Duration leaseDuration;
	private final int maxAttempts;

	public UserWithdrawalExternalCleanupWorker(
			UserWithdrawalLifecycleRepository lifecycleRepository,
			WithdrawalCleanupTargetGuard targetGuard,
			FirebaseWithdrawalCleanupPort cleanupPort,
			WithdrawalCleanupRetryPolicy retryPolicy,
			MeterRegistry meterRegistry,
			Clock clock,
			Duration leaseDuration,
			int maxAttempts
	) {
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
		this.targetGuard = Objects.requireNonNull(targetGuard);
		this.cleanupPort = Objects.requireNonNull(cleanupPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
		this.leaseDuration = requirePositive(leaseDuration, "leaseDuration");
		if (maxAttempts < 1) {
			throw new IllegalArgumentException("maxAttempts must be positive");
		}
		this.maxAttempts = maxAttempts;
	}

	public WithdrawalCleanupOutcome handoffNextCompleted() {
		boolean handedOff = lifecycleRepository.handoffNextCompleted(clock.instant()).isPresent();
		return record(
				handedOff ? WithdrawalCleanupOutcome.HANDED_OFF : WithdrawalCleanupOutcome.NONE,
				null,
				0
		);
	}

	public WithdrawalCleanupOutcome processNext() {
		Instant claimTime = clock.instant();
		String leaseToken = UUID.randomUUID().toString();
		Optional<UserWithdrawalLifecycle> claimed = lifecycleRepository.claimNext(
				leaseToken,
				claimTime,
				claimTime.plus(leaseDuration)
		);
		if (claimed.isEmpty()) {
			return record(WithdrawalCleanupOutcome.NONE, null, 0);
		}
		UserWithdrawalLifecycle lifecycle = claimed.orElseThrow();
		long version = requireVersion(lifecycle);
		WithdrawalCleanupTargetGuard.Result guard = targetGuard.verify(
				lifecycle,
				clock.instant()
		);
		if (guard.type() == WithdrawalCleanupTargetGuard.ResultType.LEASE_LOST) {
			return record(WithdrawalCleanupOutcome.LEASE_LOST, null, lifecycle.getAttemptCount());
		}
		if (guard.type() == WithdrawalCleanupTargetGuard.ResultType.RECONCILIATION_REQUIRED) {
			return reconcile(lifecycle, version, guard.failureCode(), false);
		}
		if (guard.type() == WithdrawalCleanupTargetGuard.ResultType.LOCAL_TARGET) {
			return complete(
					lifecycle,
					version,
					WithdrawalCleanupOutcome.LOCAL_TARGET_SKIPPED
			);
		}

		try {
			FirebaseCleanupAccountSnapshot snapshot = cleanupPort.inspect(
					lifecycle.getFirebaseProjectId(),
					lifecycle.getFirebaseUid()
			);
			cleanupPort.disable(lifecycle.getFirebaseProjectId(), lifecycle.getFirebaseUid());
			version = renewLease(lifecycle, version);
			if (version < 0) return leaseLost(lifecycle);

			cleanupPort.revokeRefreshTokens(
					lifecycle.getFirebaseProjectId(),
					lifecycle.getFirebaseUid()
			);
			version = renewLease(lifecycle, version);
			if (version < 0) return leaseLost(lifecycle);

			cleanupPort.satisfyProviderDeletionObligations(snapshot);
			version = renewLease(lifecycle, version);
			if (version < 0) return leaseLost(lifecycle);

			try {
				cleanupPort.delete(lifecycle.getFirebaseProjectId(), lifecycle.getFirebaseUid());
			} catch (FirebaseWithdrawalCleanupException exception) {
				if (shouldConfirmDelete(exception.failureCode())) {
					return confirmDeleteAfterUnknownResult(lifecycle, version);
				}
				throw exception;
			}
			return confirmDeleted(lifecycle, version);
		} catch (FirebaseWithdrawalCleanupException exception) {
			if (exception.failureCode() == WithdrawalCleanupFailureCode.NOT_FOUND) {
				return complete(lifecycle, version, WithdrawalCleanupOutcome.EXTERNAL_DELETED);
			}
			return handleFailure(lifecycle, version, exception.failureCode());
		}
	}

	private WithdrawalCleanupOutcome confirmDeleted(
			UserWithdrawalLifecycle lifecycle,
			long version
	) {
		try {
			FirebaseAccountPresence presence = cleanupPort.checkPresence(
					lifecycle.getFirebaseProjectId(),
					lifecycle.getFirebaseUid()
			);
			if (presence == FirebaseAccountPresence.ABSENT) {
				return complete(lifecycle, version, WithdrawalCleanupOutcome.EXTERNAL_DELETED);
			}
			return retryOrReconcile(
					lifecycle,
					version,
					WithdrawalCleanupFailureCode.DELETE_NOT_CONFIRMED
			);
		} catch (FirebaseWithdrawalCleanupException exception) {
			return handleFailure(lifecycle, version, exception.failureCode());
		}
	}

	private WithdrawalCleanupOutcome confirmDeleteAfterUnknownResult(
			UserWithdrawalLifecycle lifecycle,
			long version
	) {
		try {
			FirebaseAccountPresence presence = cleanupPort.checkPresence(
					lifecycle.getFirebaseProjectId(),
					lifecycle.getFirebaseUid()
			);
			if (presence == FirebaseAccountPresence.ABSENT) {
				return complete(lifecycle, version, WithdrawalCleanupOutcome.EXTERNAL_DELETED);
			}
			return retryOrReconcile(
					lifecycle,
					version,
					WithdrawalCleanupFailureCode.DELETE_NOT_CONFIRMED
			);
		} catch (FirebaseWithdrawalCleanupException presenceFailure) {
			return handleFailure(lifecycle, version, presenceFailure.failureCode());
		}
	}

	private WithdrawalCleanupOutcome handleFailure(
			UserWithdrawalLifecycle lifecycle,
			long version,
			WithdrawalCleanupFailureCode code
	) {
		if (code == WithdrawalCleanupFailureCode.NOT_FOUND) {
			return complete(lifecycle, version, WithdrawalCleanupOutcome.EXTERNAL_DELETED);
		}
		if (code == WithdrawalCleanupFailureCode.RESULT_UNKNOWN || code.isRetryable()) {
			return retryOrReconcile(lifecycle, version, code);
		}
		return reconcile(lifecycle, version, code, false);
	}

	private WithdrawalCleanupOutcome retryOrReconcile(
			UserWithdrawalLifecycle lifecycle,
			long version,
			WithdrawalCleanupFailureCode code
	) {
		if (lifecycle.getAttemptCount() >= maxAttempts) {
			return reconcile(lifecycle, version, code, true);
		}
		Instant now = clock.instant();
		boolean updated = lifecycleRepository.scheduleRetry(
				lifecycle.getWithdrawalId(),
				lifecycle.getLeaseOwner(),
				version,
				code,
				now.plus(retryPolicy.delay(lifecycle.getAttemptCount())),
				now
		);
		return record(
				updated ? WithdrawalCleanupOutcome.RETRY_SCHEDULED
						: WithdrawalCleanupOutcome.LEASE_LOST,
				code,
				lifecycle.getAttemptCount()
		);
	}

	private WithdrawalCleanupOutcome reconcile(
			UserWithdrawalLifecycle lifecycle,
			long version,
			WithdrawalCleanupFailureCode code,
			boolean maxAttemptsExceeded
	) {
		boolean updated = lifecycleRepository.markReconciliationRequired(
				lifecycle.getWithdrawalId(),
				lifecycle.getLeaseOwner(),
				version,
				code,
				maxAttemptsExceeded,
				clock.instant()
		);
		return record(
				updated ? WithdrawalCleanupOutcome.RECONCILIATION_REQUIRED
						: WithdrawalCleanupOutcome.LEASE_LOST,
				code,
				lifecycle.getAttemptCount()
		);
	}

	private WithdrawalCleanupOutcome complete(
			UserWithdrawalLifecycle lifecycle,
			long version,
			WithdrawalCleanupOutcome successfulOutcome
	) {
		boolean updated = lifecycleRepository.markExternalCleanupCompleted(
				lifecycle.getWithdrawalId(),
				lifecycle.getLeaseOwner(),
				version,
				clock.instant()
		);
		return record(
				updated ? successfulOutcome : WithdrawalCleanupOutcome.LEASE_LOST,
				null,
				lifecycle.getAttemptCount()
		);
	}

	private long renewLease(UserWithdrawalLifecycle lifecycle, long version) {
		Instant now = clock.instant();
		boolean renewed = lifecycleRepository.renewLease(
				lifecycle.getWithdrawalId(),
				lifecycle.getLeaseOwner(),
				version,
				now.plus(leaseDuration),
				now
		);
		return renewed ? version + 1 : -1L;
	}

	private WithdrawalCleanupOutcome leaseLost(UserWithdrawalLifecycle lifecycle) {
		return record(WithdrawalCleanupOutcome.LEASE_LOST, null, lifecycle.getAttemptCount());
	}

	private WithdrawalCleanupOutcome record(
			WithdrawalCleanupOutcome outcome,
			WithdrawalCleanupFailureCode failureCode,
			int attemptCount
	) {
		String safeFailureCode = failureCode == null ? "NONE" : failureCode.name();
		meterRegistry.counter(
				"identity.user.withdrawal.external_cleanup",
				"outcome", outcome.name(),
				"failureCode", safeFailureCode,
				"attemptBucket", attemptBucket(attemptCount)
		).increment();
		if (outcome != WithdrawalCleanupOutcome.NONE) {
			log.atInfo()
					.addKeyValue("event", "user.withdrawal.external_cleanup")
					.addKeyValue("outcome", outcome.name())
					.addKeyValue("failureCode", safeFailureCode)
					.addKeyValue("attemptBucket", attemptBucket(attemptCount))
					.log("Firebase withdrawal cleanup outcome");
		}
		return outcome;
	}

	private static boolean shouldConfirmDelete(WithdrawalCleanupFailureCode code) {
		return code == WithdrawalCleanupFailureCode.TIMEOUT
				|| code == WithdrawalCleanupFailureCode.UNAVAILABLE
				|| code == WithdrawalCleanupFailureCode.RESULT_UNKNOWN;
	}

	private static long requireVersion(UserWithdrawalLifecycle lifecycle) {
		Long version = lifecycle.getVersion();
		if (version == null || version < 0) {
			throw new IllegalStateException("Claimed lifecycle must have a version.");
		}
		return version;
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}

	private static String attemptBucket(int attemptCount) {
		if (attemptCount <= 0) return "NONE";
		if (attemptCount == 1) return "FIRST";
		if (attemptCount <= 3) return "LOW";
		if (attemptCount <= 7) return "MEDIUM";
		return "HIGH";
	}
}
