package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;
import web.tosunsaeng.identity.domain.auth.federation.repository.AbandonedFirebaseEnrollmentCleanupRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public final class AbandonedFirebaseEnrollmentTargetGuard {

	public record Result(boolean allowed, boolean leaseLost,
	                     AbandonedFirebaseEnrollmentFailureCode failureCode) {
		public Result {
			if (allowed && (leaseLost || failureCode != null)) throw new IllegalArgumentException();
			if (leaseLost && failureCode != null) throw new IllegalArgumentException();
			if (!allowed && !leaseLost && failureCode == null) throw new IllegalArgumentException();
		}
		public static Result allowedResult() { return new Result(true, false, null); }
		public static Result leaseLostResult() { return new Result(false, true, null); }
		public static Result reconcile(AbandonedFirebaseEnrollmentFailureCode code) {
			return new Result(false, false, Objects.requireNonNull(code));
		}
	}

	private final AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository;
	private final FirebaseEnrollmentAttemptRepository attemptRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final UserRepository userRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneNumberNormalizer phoneNormalizer;
	private final PhoneFingerprintHasher phoneHasher;

	public AbandonedFirebaseEnrollmentTargetGuard(
			AbandonedFirebaseEnrollmentCleanupRepository cleanupRepository,
			FirebaseEnrollmentAttemptRepository attemptRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneNumberNormalizer phoneNormalizer,
			PhoneFingerprintHasher phoneHasher
	) {
		this.cleanupRepository = Objects.requireNonNull(cleanupRepository);
		this.attemptRepository = Objects.requireNonNull(attemptRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.phoneNormalizer = Objects.requireNonNull(phoneNormalizer);
		this.phoneHasher = Objects.requireNonNull(phoneHasher);
	}

	public Result verifyLocal(AbandonedFirebaseEnrollmentCleanup claim, Instant now) {
		AbandonedFirebaseEnrollmentCleanup required = Objects.requireNonNull(claim);
		Instant requiredNow = Objects.requireNonNull(now);
		Optional<AbandonedFirebaseEnrollmentCleanup> current = cleanupRepository.findById(
				required.getCleanupId()
		);
		if (current.isEmpty() || !sameClaim(required, current.orElseThrow(), requiredNow)) {
			return Result.leaseLostResult();
		}

		List<FirebaseEnrollmentAttempt> attempts = attemptRepository
				.findAllByFirebaseProjectIdAndFirebaseUidOrderByCreatedAtDesc(
						required.getFirebaseProjectId(), required.getFirebaseUid()
				);
		Optional<FirebaseEnrollmentAttempt> source = attempts.stream()
				.filter(attempt -> required.getSourceEnrollmentId().equals(attempt.getEnrollmentId()))
				.findFirst();
		if (source.isEmpty()
				|| source.orElseThrow().getStatus() == FirebaseEnrollmentStatus.CONSUMED
				|| !source.orElseThrow().isExpiredAt(requiredNow)
				|| attempts.stream().anyMatch(attempt ->
						attempt.getStatus() == FirebaseEnrollmentStatus.CONSUMED
								|| attempt.isActiveAt(requiredNow))) {
			return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.ATTEMPT_STATE_CONFLICT);
		}
		if (firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				required.getFirebaseProjectId(), required.getFirebaseUid()
		).isPresent()) {
			return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.OWNER_PRESENT);
		}
		if (required.getBindingType() == FirebaseEnrollmentBindingType.GUEST_USER) {
			var user = userRepository.findById(required.getBoundUserId());
			if (user.isEmpty()
					|| user.orElseThrow().getStatus() != UserStatus.ACTIVE
					|| user.orElseThrow().getAccountType() != UserAccountType.GUEST
					|| firebaseIdentityRepository.findByUserId(required.getBoundUserId()).isPresent()) {
				return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.BOUND_GUEST_INVALID);
			}
		}
		return Result.allowedResult();
	}

	public Result verifyExternalOwners(AbandonedFirebaseAccountSnapshot snapshot) {
		AbandonedFirebaseAccountSnapshot required = Objects.requireNonNull(snapshot);
		for (AbandonedFirebaseLinkedProvider linked : required.linkedProviders()) {
			if (socialIdentityRepository.findByProviderAndProviderSubject(
					linked.provider(), linked.providerSubject()
			).isPresent()) {
				return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.OWNER_PRESENT);
			}
		}
		if (required.verifiedPhoneNumber() != null) {
			try {
				var fingerprints = phoneHasher.fingerprint(
						phoneNormalizer.normalize(required.verifiedPhoneNumber())
				);
				if (!aliasRepository.findAllActiveByFingerprints(fingerprints.retained()).isEmpty()) {
					return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.OWNER_PRESENT);
				}
			} catch (IllegalArgumentException exception) {
				return Result.reconcile(AbandonedFirebaseEnrollmentFailureCode.INVARIANT_VIOLATION);
			}
		}
		return Result.allowedResult();
	}

	private static boolean sameClaim(
			AbandonedFirebaseEnrollmentCleanup claimed,
			AbandonedFirebaseEnrollmentCleanup current,
			Instant now
	) {
		return current.getStatus() == AbandonedFirebaseEnrollmentCleanupStatus.CLEANUP_IN_PROGRESS
				&& current.getGeneration() == claimed.getGeneration()
				&& Objects.equals(current.getLeaseOwner(), claimed.getLeaseOwner())
				&& Objects.equals(current.getVersion(), claimed.getVersion())
				&& current.getLeaseUntil() != null
				&& current.getLeaseUntil().isAfter(now);
	}
}
