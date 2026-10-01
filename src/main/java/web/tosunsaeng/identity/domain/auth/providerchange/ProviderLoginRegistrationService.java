package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Clock;
import java.util.function.Supplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard.*;

/** LOGIN_EXCHANGE only. Local registration and token/session issuance commit together. */
public class ProviderLoginRegistrationService {
	private final MongoTemplate mongo;
	private final SessionSecurityService security;
	private final ProviderChangeGuard guard;
	private final FirebaseIdentityRepository identities;
	private final SocialIdentityRepository socials;
	private final UserRepository users;
	private final Clock clock;

	public ProviderLoginRegistrationService(MongoTemplate mongo, SessionSecurityService security,
			ProviderChangeGuard guard, FirebaseIdentityRepository identities, SocialIdentityRepository socials,
			UserRepository users, Clock clock) {
		this.mongo = mongo; this.security = security; this.guard = guard; this.identities = identities;
		this.socials = socials; this.users = users; this.clock = clock;
	}

	public <T> T authenticate(FirebaseIdentity snapshot, VerifiedFirebasePrincipal proof, Supplier<T> issue) {
		long epoch = security.captureEpoch(snapshot.getUserId());
		try {
			return security.transactionKeepingUniqueConflicts(() -> {
				var binding = identities.findByFirebaseProjectIdAndFirebaseUid(proof.firebaseProjectId(), proof.firebaseUid())
						.orElseThrow(() -> error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
				if (!binding.getFirebaseIdentityId().equals(snapshot.getFirebaseIdentityId())
						|| !binding.getUserId().equals(snapshot.getUserId())
						|| !binding.getCreatedAt().equals(snapshot.getCreatedAt())) {
					throw error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
				}
				String userId = binding.getUserId();
				security.requireActive(userId);
				if (!users.findById(userId).orElseThrow(() -> error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT)).isMember()) {
					throw error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
				}
				guard.authenticate(userId, binding.getFirebaseIdentityId(), proof.signInMethod(), proof.authTime());
				security.checkFirebaseAuthentication(userId, security.firebase(userId, epoch, proof));
				SocialProvider provider = social(proof.signInMethod());
				if (supportsFirstLoginRegistration(provider)) {
					var targets = proof.linkedSocialPrincipals().stream().filter(p -> p.provider() == provider).toList();
					if (targets.size() != 1) throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
					var target = targets.getFirst();
					var existing = socials.findByProviderAndProviderSubject(provider, target.providerSubject());
					if (existing.isPresent() && !userId.equals(existing.orElseThrow().getUserId())) {
						throw error(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
					}
					if (existing.isEmpty()) {
						if (security.control(userId).getActiveLogoutId() != null || security.hasUnresolvedLogout(userId)) {
							throw error(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT);
						}
						var methods = guard.control(userId, binding.getFirebaseIdentityId());
						if (methods.isBlocked(provider) || methods.getAuthenticationFloors().containsKey(provider.name())
								|| socials.findAllByUserId(userId).stream().anyMatch(s -> s.getProvider() == provider)) {
							throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
						}
						socials.save(SocialIdentity.create(userId, provider, target.providerSubject(), clock.instant()));
						methods.registerFirstProvider();
						mongo.save(methods);
					}
				}
				return issue.get();
			});
		} catch (DuplicateKeyException exception) {
			// A competing owner/control insert must never be treated as successful authentication.
			// A new exchange re-reads ownership and converges if the same member won.
			throw error(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT);
		}
	}
}
