package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

public class AbandonedFirebaseCleanupTerminalTransactionService {

	private final AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository;
	private final FirebaseEnrollmentAttemptRepository attemptRepository;
	private final Duration lifecycleRetention;
	private final Duration attemptRetention;

	public AbandonedFirebaseCleanupTerminalTransactionService(
			AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
			FirebaseEnrollmentAttemptRepository attemptRepository,
			Duration lifecycleRetention,
			Duration attemptRetention
	) {
		this.cleanupRepository = Objects.requireNonNull(cleanupRepository);
		this.attemptRepository = Objects.requireNonNull(attemptRepository);
		this.lifecycleRetention = requirePositive(lifecycleRetention, "lifecycleRetention");
		this.attemptRetention = requirePositive(attemptRetention, "attemptRetention");
		if (this.lifecycleRetention.compareTo(this.attemptRetention) <= 0) {
			throw new IllegalArgumentException("Lifecycle retention must exceed attempt retention.");
		}
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public boolean markCleaned(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			long expectedVersion,
			Instant terminalAt
	) {
		AbandonedFirebaseEnrollmentCleanup required = Objects.requireNonNull(cleanup);
		Instant requiredTerminalAt = Objects.requireNonNull(terminalAt);
		boolean updated = cleanupRepository.markCleaned(
				required.getCleanupId(),
				required.getLeaseOwner(),
				required.getGeneration(),
				expectedVersion,
				requiredTerminalAt,
				requiredTerminalAt.plus(lifecycleRetention)
		);
		if (!updated) return false;
		long scheduled = attemptRepository.scheduleCleanupForTarget(
				required.getFirebaseProjectId(),
				required.getFirebaseUid(),
				requiredTerminalAt.plus(attemptRetention)
		);
		if (scheduled < 1) {
			throw new IllegalStateException("Cleaned enrollment source could not be retained.");
		}
		return true;
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}
}
