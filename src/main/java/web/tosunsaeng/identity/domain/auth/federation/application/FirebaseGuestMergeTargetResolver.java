package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public final class FirebaseGuestMergeTargetResolver {

	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final UserRepository userRepository;
	private final WithdrawalEnrollmentGate withdrawalEnrollmentGate;

	public FirebaseGuestMergeTargetResolver(
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository
	) {
		this(firebaseIdentityRepository, socialIdentityRepository, userRepository, null);
	}

	public FirebaseGuestMergeTargetResolver(
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository,
			WithdrawalEnrollmentGate withdrawalEnrollmentGate
	) {
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.withdrawalEnrollmentGate = withdrawalEnrollmentGate;
	}

	public User resolve(VerifiedFirebasePrincipal principal, String sourceUserId) {
		VerifiedFirebasePrincipal requiredPrincipal = Objects.requireNonNull(principal);
		String requiredSourceUserId = Objects.requireNonNull(sourceUserId);
		Set<String> ownerIds = new HashSet<>();
		firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				requiredPrincipal.firebaseProjectId(),
				requiredPrincipal.firebaseUid()
		).map(FirebaseIdentity::getUserId).ifPresent(ownerIds::add);
		for (VerifiedSocialPrincipal social : requiredPrincipal.linkedSocialPrincipals()) {
			socialIdentityRepository.findByProviderAndProviderSubject(
					social.provider(),
					social.providerSubject()
			).map(SocialIdentity::getUserId).ifPresent(ownerIds::add);
		}
		ownerIds.forEach(this::checkOwner);
		if (ownerIds.size() != 1 || ownerIds.contains(requiredSourceUserId)) {
			throw conflict();
		}
		User target = userRepository.findById(ownerIds.iterator().next())
				.orElseThrow(this::conflict);
		if (target.getStatus() != UserStatus.ACTIVE
				|| !target.isMember()
				|| target.getMergedIntoUserId() != null) {
			throw conflict();
		}
		return target;
	}

	private AuthException conflict() {
		return new AuthException(AuthErrorStatus.GUEST_MERGE_TARGET_CONFLICT);
	}

	private void checkOwner(String ownerUserId) {
		if (withdrawalEnrollmentGate != null) {
			withdrawalEnrollmentGate.checkExistingOwner(ownerUserId);
		}
	}
}
