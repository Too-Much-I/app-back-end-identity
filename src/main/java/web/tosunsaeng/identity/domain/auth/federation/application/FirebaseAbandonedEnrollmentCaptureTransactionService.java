package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public class FirebaseAbandonedEnrollmentCaptureTransactionService {

	private final FirebaseEnrollmentAttemptRepository attemptRepository;
	private final AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final UserRepository userRepository;

	public FirebaseAbandonedEnrollmentCaptureTransactionService(
			FirebaseEnrollmentAttemptRepository attemptRepository,
			AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			UserRepository userRepository
	) {
		this.attemptRepository = Objects.requireNonNull(attemptRepository);
		this.cleanupRepository = Objects.requireNonNull(cleanupRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager", readOnly = false)
	public boolean captureIfEligible(
			FirebaseEnrollmentAttempt candidate,
			Duration grace,
			Instant capturedAt,
			boolean dryRun
	) {
		FirebaseEnrollmentAttempt required = Objects.requireNonNull(candidate);
		Instant now = Objects.requireNonNull(capturedAt);
		if (!required.isExpiredAt(now)
				|| (required.getStatus() != FirebaseEnrollmentStatus.PENDING
				&& required.getStatus() != FirebaseEnrollmentStatus.EXPIRED)) return false;
		if (cleanupRepository.findByFirebaseProjectIdAndFirebaseUid(
				required.getFirebaseProjectId(), required.getFirebaseUid()
		).isPresent()) return false;

		List<FirebaseEnrollmentAttempt> history = attemptRepository
				.findAllByFirebaseProjectIdAndFirebaseUidOrderByCreatedAtDesc(
						required.getFirebaseProjectId(), required.getFirebaseUid()
				);
		if (history.isEmpty()
				|| !history.getFirst().getEnrollmentId().equals(required.getEnrollmentId())
				|| history.stream().anyMatch(attempt ->
						attempt.getStatus() == FirebaseEnrollmentStatus.CONSUMED
								|| attempt.isActiveAt(now))) return false;
		if (firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				required.getFirebaseProjectId(), required.getFirebaseUid()
		).isPresent()) return false;
		if (required.getBindingType() == FirebaseEnrollmentBindingType.GUEST_USER) {
			var guest = userRepository.findById(required.getBoundUserId());
			if (guest.isEmpty()
					|| guest.orElseThrow().getStatus() != UserStatus.ACTIVE
					|| guest.orElseThrow().getAccountType() != UserAccountType.GUEST
					|| firebaseIdentityRepository.findByUserId(required.getBoundUserId()).isPresent()) {
				return false;
			}
		}
		if (dryRun) return true;

		cleanupRepository.save(AbandonedFirebaseEnrollmentCleanup.createResumable(
				required, grace, now
		));
		attemptRepository.clearCleanupAtForTarget(
				required.getFirebaseProjectId(), required.getFirebaseUid()
		);
		return true;
	}
}
