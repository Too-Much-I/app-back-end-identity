package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneIdentityTransactionService;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public class FirebaseGuestUpgradeTransactionService {

	private final UserRepository userRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final PhoneIdentityTransactionService phoneIdentityTransactionService;
	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseEnrollmentAttemptRepository enrollmentRepository;

	public FirebaseGuestUpgradeTransactionService(
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneIdentityTransactionService phoneIdentityTransactionService,
			RefreshSessionRepository refreshSessionRepository,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptRepository enrollmentRepository
	) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.phoneIdentityTransactionService = Objects.requireNonNull(
				phoneIdentityTransactionService
		);
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.enrollmentRepository = Objects.requireNonNull(enrollmentRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public IssuedRefreshSession upgrade(
			User promotedUser,
			Instant expectedUpdatedAt,
			FirebaseIdentity firebaseIdentityToCreate,
			PhoneFingerprintSet phoneFingerprints,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates,
			List<SocialIdentity> socialIdentitiesToCreate,
			PreparedRefreshSession preparedRefreshSession,
			FirebaseEnrollmentAttempt enrollmentAttempt,
			Instant upgradedAt
	) {
		User requiredUser = Objects.requireNonNull(promotedUser);
		String userId = requiredUser.getUserId();
		if (!userId.equals(preparedRefreshSession.session().getUserId())
				|| (firebaseIdentityToCreate != null
						&& !userId.equals(firebaseIdentityToCreate.getUserId()))
				|| socialIdentitiesToCreate.stream()
						.anyMatch(identity -> !userId.equals(identity.getUserId()))) {
			throw new IllegalArgumentException("Guest upgrade aggregate user IDs must match.");
		}
		if (!userRepository.promoteGuestIfUnchanged(requiredUser, expectedUpdatedAt)) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
		}
		if (firebaseIdentityToCreate != null) {
			firebaseIdentityRepository.save(firebaseIdentityToCreate);
		}
		phoneIdentityTransactionService.linkOrReplace(
				userId,
				Objects.requireNonNull(phoneFingerprints),
				Objects.requireNonNull(consumerScopeId),
				List.copyOf(Objects.requireNonNull(eligibilityCandidates)),
				Objects.requireNonNull(upgradedAt)
		);
		if (!socialIdentitiesToCreate.isEmpty()) {
			socialIdentityRepository.saveAll(socialIdentitiesToCreate);
		}
		List<RefreshSession> activeGuestSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(userId);
		for (RefreshSession session : activeGuestSessions) {
			session.upgradeGuestAccount(upgradedAt);
		}
		if (!activeGuestSessions.isEmpty()) {
			refreshSessionRepository.saveAll(activeGuestSessions);
		}
		IssuedRefreshSession issuedRefreshSession = refreshSessionIssuer.savePrepared(
				preparedRefreshSession
		);
		boolean consumed = enrollmentRepository.consumeIfPendingAndNotExpired(
				enrollmentAttempt.getEnrollmentId(),
				enrollmentAttempt.getFirebaseProjectId(),
				enrollmentAttempt.getFirebaseUid(),
				FirebaseEnrollmentBindingType.GUEST_USER,
				userId,
				upgradedAt
		);
		if (!consumed) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
		}
		return issuedRefreshSession;
	}
}
