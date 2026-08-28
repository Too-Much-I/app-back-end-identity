package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

public final class FirebaseEnrollmentLifecycleService {

	private final AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository;
	private final FirebaseEnrollmentAttemptRepository attemptRepository;
	private final Duration lifecycleRetention;
	private final Duration attemptRetention;

	public FirebaseEnrollmentLifecycleService(
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

	public void finalizeEnrollment(FirebaseEnrollmentAttempt attempt, Instant terminalAt) {
		FirebaseEnrollmentAttempt requiredAttempt = Objects.requireNonNull(attempt);
		Instant requiredTerminalAt = Objects.requireNonNull(terminalAt);
		Optional<AbandonedFirebaseEnrollmentCleanup> cleanup = cleanupRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						requiredAttempt.getFirebaseProjectId(), requiredAttempt.getFirebaseUid()
				);
		if (cleanup.isEmpty()) {
			cleanupRepository.save(AbandonedFirebaseEnrollmentCleanup.createFinalized(
					requiredAttempt, requiredTerminalAt, lifecycleRetention
			));
		} else if (!cleanupRepository.markFinalizedIfResumable(
				requiredAttempt.getFirebaseProjectId(),
				requiredAttempt.getFirebaseUid(),
				requiredAttempt.getEnrollmentId(),
				requiredTerminalAt,
				requiredTerminalAt.plus(lifecycleRetention)
		)) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_RESTART_REQUIRED);
		}
		long scheduled = attemptRepository.scheduleCleanupForTarget(
				requiredAttempt.getFirebaseProjectId(),
				requiredAttempt.getFirebaseUid(),
				requiredTerminalAt.plus(attemptRetention)
		);
		if (scheduled < 1) {
			throw new IllegalStateException("Terminal enrollment source could not be retained.");
		}
	}

	public void ensureFinalizable(FirebaseEnrollmentAttempt attempt) {
		FirebaseEnrollmentAttempt required = Objects.requireNonNull(attempt);
		cleanupRepository.findByFirebaseProjectIdAndFirebaseUid(
				required.getFirebaseProjectId(), required.getFirebaseUid()
		).ifPresent(cleanup -> {
			if (cleanup.getStatus() != AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE
					|| !cleanup.getSourceEnrollmentId().equals(required.getEnrollmentId())
					|| cleanup.getBindingType() != required.getBindingType()
					|| !Objects.equals(cleanup.getBoundUserId(), required.getBoundUserId())) {
				throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_RESTART_REQUIRED);
			}
		});
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}
}
