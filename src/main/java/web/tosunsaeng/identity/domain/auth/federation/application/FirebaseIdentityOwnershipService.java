package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
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

public final class FirebaseIdentityOwnershipService {

	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final UserRepository userRepository;

	public FirebaseIdentityOwnershipService(
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository
	) {
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
	}

	public FirebaseOwnershipOutcome resolve(
			VerifiedFirebasePrincipal principal,
			String currentUserId
	) {
		VerifiedFirebasePrincipal requiredPrincipal = Objects.requireNonNull(principal);
		String requiredCurrentUserId = Objects.requireNonNull(currentUserId);
		Set<String> ownerIds = new HashSet<>();
		Optional<FirebaseIdentity> firebaseIdentity = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
					requiredPrincipal.firebaseProjectId(),
					requiredPrincipal.firebaseUid()
				);
		firebaseIdentity.map(FirebaseIdentity::getUserId).ifPresent(ownerIds::add);
		for (VerifiedSocialPrincipal socialPrincipal : requiredPrincipal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> socialIdentity = socialIdentityRepository
					.findByProviderAndProviderSubject(
						socialPrincipal.provider(),
						socialPrincipal.providerSubject()
					);
			socialIdentity.map(SocialIdentity::getUserId).ifPresent(ownerIds::add);
		}
		if (ownerIds.isEmpty()) {
			return FirebaseOwnershipOutcome.UNOWNED;
		}
		Set<String> otherOwnerIds = new HashSet<>(ownerIds);
		otherOwnerIds.remove(requiredCurrentUserId);
		if (otherOwnerIds.isEmpty()) {
			return FirebaseOwnershipOutcome.OWNED_BY_CURRENT_USER;
		}
		if (otherOwnerIds.size() != 1) {
			throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		}
		String ownerId = otherOwnerIds.iterator().next();
		User owner = userRepository.findById(ownerId)
				.orElseThrow(() -> new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		if (owner.getStatus() == UserStatus.ACTIVE && owner.isMember()) {
			return FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER;
		}
		throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}
}
