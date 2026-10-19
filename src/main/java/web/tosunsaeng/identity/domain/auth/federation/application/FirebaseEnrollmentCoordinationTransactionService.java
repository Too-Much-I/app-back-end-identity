package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;

public class FirebaseEnrollmentCoordinationTransactionService {

	private final FirebaseEnrollmentAttemptRepository attemptRepository;
	private final AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository;
	private final Clock clock;
	private final Duration enrollmentTtl;
	private final Duration grace;

	public FirebaseEnrollmentCoordinationTransactionService(
			FirebaseEnrollmentAttemptRepository attemptRepository,
			AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
			Clock clock,
			Duration enrollmentTtl,
			Duration grace
	) {
		this.attemptRepository = Objects.requireNonNull(attemptRepository);
		this.cleanupRepository = Objects.requireNonNull(cleanupRepository);
		this.clock = Objects.requireNonNull(clock);
		this.enrollmentTtl = requirePositive(enrollmentTtl, "enrollmentTtl");
		this.grace = requirePositive(grace, "grace");
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public FirebaseEnrollmentAttempt startOrReuse(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			FirebaseAuthenticationMethod initialSignInMethod
	) {
		Instant now = clock.instant();
		Optional<AbandonedFirebaseEnrollmentCleanup> cleanup = cleanupRepository
				.findByFirebaseProjectIdAndFirebaseUid(firebaseProjectId, firebaseUid);
		if (cleanup.isPresent()) {
			ensureResumableBinding(cleanup.orElseThrow(), bindingType, boundUserId);
		}

		Optional<FirebaseEnrollmentAttempt> pending = attemptRepository.findPendingByBinding(
				firebaseProjectId, firebaseUid, bindingType, boundUserId
		);
		if (pending.isPresent() && pending.orElseThrow().isActiveAt(now)) {
			FirebaseEnrollmentAttempt existing = pending.orElseThrow();
			coordinateExistingAttempt(cleanup, existing, now);
			return existing;
		}
		pending.ifPresent(existing -> attemptRepository.expireIfPendingAndExpired(
				existing.getEnrollmentId(), now
		));

		FirebaseEnrollmentAttempt candidate = FirebaseEnrollmentAttempt.create(
				firebaseProjectId,
				firebaseUid,
				bindingType,
				boundUserId,
				initialSignInMethod,
				now,
				enrollmentTtl
		);
		attemptRepository.save(candidate);
		attemptRepository.clearCleanupAtForTarget(firebaseProjectId, firebaseUid);
		if (cleanup.isEmpty()) {
			cleanupRepository.save(AbandonedFirebaseEnrollmentCleanup.createResumable(
					candidate, grace, now
			));
		} else {
			AbandonedFirebaseEnrollmentCleanup existingCleanup = cleanup.orElseThrow();
			existingCleanup.resume(candidate, grace, now);
			cleanupRepository.save(existingCleanup);
		}
		return candidate;
	}

	private void coordinateExistingAttempt(
			Optional<AbandonedFirebaseEnrollmentCleanup> cleanup,
			FirebaseEnrollmentAttempt attempt,
			Instant now
	) {
		if (cleanup.isEmpty()) {
			attemptRepository.clearCleanupAtForTarget(
					attempt.getFirebaseProjectId(), attempt.getFirebaseUid()
			);
			cleanupRepository.save(AbandonedFirebaseEnrollmentCleanup.createResumable(
					attempt, grace, now
			));
			return;
		}
		AbandonedFirebaseEnrollmentCleanup existing = cleanup.orElseThrow();
		if (!existing.getSourceEnrollmentId().equals(attempt.getEnrollmentId())) {
			existing.resume(attempt, grace, now);
			cleanupRepository.save(existing);
		}
	}

	private void ensureResumableBinding(
			AbandonedFirebaseEnrollmentCleanup cleanup,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		if (cleanup.getStatus() != AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE
				|| cleanup.getBindingType() != bindingType
				|| !Objects.equals(cleanup.getBoundUserId(), boundUserId)) {
			throw restartRequired();
		}
	}

	private static AuthException restartRequired() {
		return new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_RESTART_REQUIRED);
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return required;
	}
}
