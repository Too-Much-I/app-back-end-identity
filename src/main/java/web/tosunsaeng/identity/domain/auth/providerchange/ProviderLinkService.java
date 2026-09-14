package web.tosunsaeng.identity.domain.auth.providerchange;

import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard.*;
import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeService.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;

/** One intent, one client mutation grant, one target-only local completion. */
public class ProviderLinkService {
	private web.tosunsaeng.identity.domain.auth.federation.application.WithdrawalEnrollmentGate withdrawalGate;
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setWithdrawalGate(web.tosunsaeng.identity.domain.auth.federation.application.WithdrawalEnrollmentGate gate) { withdrawalGate = gate; }
	public record Status(String linkAttemptId, SocialProvider provider, String status, Instant expiresAt,
			boolean linkAllowed) { }
	private final ProviderChangeService changes;
	private final MongoTemplate mongo;
	private final SessionSecurityService security;
	private final ProviderChangeGuard guard;
	private final SocialIdentityRepository socials;
	private final ProviderChangeProperties properties;
	private final Clock clock;

	public ProviderLinkService(ProviderChangeService changes, MongoTemplate mongo, SessionSecurityService security,
			ProviderChangeGuard guard, SocialIdentityRepository socials, ProviderChangeProperties properties, Clock clock) {
		this.changes = changes; this.mongo = mongo; this.security = security; this.guard = guard;
		this.socials = socials; this.properties = properties; this.clock = clock;
	}

	public Status prepare(String userId, SocialProvider provider, String token, List<String> keys) {
		if (!properties.isLinkEnabled()) throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		Objects.requireNonNull(provider);
		String hash = requestHash(keys);
		long epoch = security.captureEpoch(userId), revision = changes.captureRevision(userId);
		changes.rateLimit(userId);
		var proof = changes.verify(token); var snapshot = changes.binding(userId, proof);
		return changes.transaction(() -> {
			var binding = changes.binding(userId, proof); exactBinding(snapshot, binding);
			var old = mongo.findOne(Query.query(Criteria.where("userId").is(userId).and("requestIdHash").is(hash)), ProviderLinkAttempt.class);
			if (old != null) {
				match(old, userId, binding); if (old.getProvider() != provider) conflict();
				authorizeStatus(old, proof); return view(old, false);
			}
			var control = security.control(userId); control.requireEpoch(epoch);
			var methods = guard.control(userId, binding.getFirebaseIdentityId());
			if (revision != methods.getRevision() || control.getActiveLogoutId() != null || security.hasUnresolvedLogout(userId)) conflict();
			if (!methods.isBlocked(provider) && proof.linkedSocialPrincipals().stream().anyMatch(p -> p.provider() == provider)) {
				var owned = changes.ownedSocial(userId, provider, proof);
				changes.ownedSocial(userId, social(proof.signInMethod()), proof);
				security.checkFirebaseAuthentication(userId, security.firebase(userId, epoch, proof));
				Instant now = clock.instant();
				var receipt = ProviderLinkAttempt.create(binding, provider, hash, epoch, revision, now,
						now.plus(properties.getPermitTtl()), now.plus(properties.getPermitRetention()));
				receipt.alreadyLinked(now, owned.getSocialIdentityId(), now.plus(properties.getPermitRetention()));
				mongo.insert(receipt);
				return view(receipt, false);
			}
			remainingProof(userId, provider, proof, binding);
			// A remote link made before prepare is not accepted retroactively.
			if (proof.linkedSocialPrincipals().stream().anyMatch(p -> p.provider() == provider)
					|| socials.findAllByUserId(userId).stream().anyMatch(p -> p.getProvider() == provider)) conflict();
			security.checkFirebaseAuthentication(userId, security.firebase(userId, epoch, proof));
			Instant now = clock.instant(), expires = now.plus(properties.getPermitTtl());
			var attempt = ProviderLinkAttempt.create(binding, provider, hash, epoch, revision, now, expires,
					expires.plus(properties.getPermitRetention()));
			mongo.insert(attempt); // PREPARED deliberately claims no remote mutation slot.
			return view(attempt, false);
		});
	}

	public Status start(String userId, String attemptId, String token) {
		if (!properties.isLinkEnabled()) throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		changes.rateLimit(userId);
		var proof = changes.verify(token); var snapshot = changes.binding(userId, proof);
		return changes.transaction(() -> {
			var binding = changes.binding(userId, proof); exactBinding(snapshot, binding);
			var a = find(attemptId, userId, binding);
			remainingProof(userId, a.getProvider(), proof, binding);
			if (a.getState() != ProviderLinkAttempt.State.PREPARED) {
				// A replay NEVER grants another SDK call. Lost response must use status/remote inspection.
				authorizeStatus(a, proof); return view(a, false);
			}
			if (!clock.instant().isBefore(a.getExpiresAt())) throw error(AuthErrorStatus.PROVIDER_RELINK_EXPIRED);
			var control = security.control(userId); control.requireEpoch(a.getEpoch());
			var methods = guard.control(userId, binding.getFirebaseIdentityId());
			if (methods.getRevision() != a.getRevision() || control.getActiveLogoutId() != null || security.hasUnresolvedLogout(userId)) conflict();
			if (proof.linkedSocialPrincipals().stream().anyMatch(p -> p.provider() == a.getProvider())
					|| socials.findAllByUserId(userId).stream().anyMatch(p -> p.getProvider() == a.getProvider())) conflict();
			security.checkFirebaseAuthentication(userId, security.firebase(userId, a.getEpoch(), proof));
			control = security.control(userId);
			Instant now = clock.instant();
			methods.block(a.getProvider(), now); // Also fence FIRST links until target-only completion.
			a.start(now, methods.getRevision(), now.plus(properties.getPermitTtl()));
			control.claimLogout(a.slot());
			mongo.save(control); mongo.save(methods); mongo.save(a);
			return view(a, true);
		});
	}

	/** Completing already started work remains available when new link capture is disabled. */
	public Status complete(String userId, String attemptId, String token) {
		changes.rateLimit(userId);
		var proof = changes.verify(token); var snapshot = changes.binding(userId, proof);
		return changes.transaction(() -> {
			var binding = changes.binding(userId, proof); exactBinding(snapshot, binding);
			var a = find(attemptId, userId, binding);
			var control = security.control(userId); control.requireEpoch(a.getEpoch());
			var methods = guard.control(userId, binding.getFirebaseIdentityId());
			if (social(proof.signInMethod()) != a.getProvider()) throw error(AuthErrorStatus.PROVIDER_REMAINING_AUTH_REQUIRED);
			if (a.getState() == ProviderLinkAttempt.State.COMPLETED) {
				if (methods.getRevision() != a.getRevision() + 1 || methods.isBlocked(a.getProvider())) conflict();
				var owned = changes.ownedSocial(userId, a.getProvider(), proof);
				if (!owned.getSocialIdentityId().equals(a.getSocialIdentityId())) conflict();
				security.checkFirebaseAuthentication(userId, security.firebase(userId, a.getEpoch(), proof));
				return view(a, false);
			}
			if (a.getState() != ProviderLinkAttempt.State.STARTED) conflict();
			if (!clock.instant().isBefore(a.getExpiresAt())) throw error(AuthErrorStatus.PROVIDER_RELINK_EXPIRED);
			if (!a.slot().equals(control.getActiveLogoutId()) || methods.getRevision() != a.getRevision()
					|| !methods.isBlocked(a.getProvider()) || !proof.authTime().isAfter(a.getStartedAt())) conflict();
			var target = proof.linkedSocialPrincipals().stream().filter(p -> p.provider() == a.getProvider()).findFirst()
					.orElseThrow(() -> error(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT));
			// Never synchronize other providers found in this Firebase response.
			var existing = socials.findByProviderAndProviderSubject(target.provider(), target.providerSubject());
			if (existing.isPresent() && !userId.equals(existing.orElseThrow().getUserId()) && withdrawalGate != null) {
				withdrawalGate.checkExistingOwner(existing.orElseThrow().getUserId());
			}
			if (existing.isPresent() || socials.findAllByUserId(userId).stream().anyMatch(p -> p.getProvider() == target.provider())) {
				throw error(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
			control.validate(a.getEpoch(), security.firebase(userId, a.getEpoch(), proof), true);
			Instant now = clock.instant();
			var identity = socials.save(SocialIdentity.create(userId, target.provider(), target.providerSubject(), now));
			methods.release(a.getProvider(), a.getStartedAt());
			a.complete(now, identity.getSocialIdentityId(), now.plus(properties.getPermitRetention()));
			control.releaseLogout(a.slot());
			mongo.save(methods); mongo.save(a); mongo.save(control);
			return view(a, false);
		});
	}

	/** Lookup by the original prepare request ID also recovers a lost prepare response. */
	public Status status(String userId, String requestId, String token) {
		String hash = requestHash(java.util.Collections.singletonList(requestId));
		changes.rateLimit(userId); var proof = changes.verify(token); var snapshot = changes.binding(userId, proof);
		return changes.transaction(() -> {
			var binding = changes.binding(userId, proof); exactBinding(snapshot, binding);
			var a = mongo.findOne(Query.query(Criteria.where("userId").is(userId).and("requestIdHash").is(hash)), ProviderLinkAttempt.class);
			if (a == null) throw error(AuthErrorStatus.PROVIDER_OPERATION_NOT_FOUND);
			match(a, userId, binding); authorizeStatus(a, proof);
			return view(a, false);
		});
	}

	private void remainingProof(String userId, SocialProvider target, VerifiedFirebasePrincipal proof, FirebaseIdentity binding) {
		if (!changes.remaining(userId, proof, target, guard.control(userId, binding.getFirebaseIdentityId())).contains(social(proof.signInMethod()))) {
			throw error(AuthErrorStatus.PROVIDER_REMAINING_AUTH_REQUIRED);
		}
		guard.authenticate(userId, binding.getFirebaseIdentityId(), proof.signInMethod(), proof.authTime());
	}
	private void authorizeStatus(ProviderLinkAttempt a, VerifiedFirebasePrincipal proof) {
		// Target proof can inspect its own STARTED operation but cannot issue an application token.
		if ((a.getState() == ProviderLinkAttempt.State.STARTED || a.getState() == ProviderLinkAttempt.State.COMPLETED)
				&& social(proof.signInMethod()) == a.getProvider()) {
			if (!proof.authTime().isAfter(a.getStartedAt())) conflict();
		} else {
			changes.ownedSocial(a.getUserId(), social(proof.signInMethod()), proof);
			guard.authenticate(a.getUserId(), a.getBindingId(), proof.signInMethod(), proof.authTime());
		}
		var control = security.control(a.getUserId());
		control.validate(control.getSessionEpoch(), security.firebase(a.getUserId(), control.getSessionEpoch(), proof), true);
	}
	private ProviderLinkAttempt find(String id, String userId, FirebaseIdentity binding) {
		var a = mongo.findById(uuid(id), ProviderLinkAttempt.class);
		if (a == null) throw error(AuthErrorStatus.PROVIDER_OPERATION_NOT_FOUND);
		match(a, userId, binding); return a;
	}
	private void match(ProviderLinkAttempt a, String userId, FirebaseIdentity binding) {
		if (!userId.equals(a.getUserId()) || !binding.getFirebaseIdentityId().equals(a.getBindingId())
				|| !binding.getCreatedAt().equals(a.getBindingCreatedAt()) || !binding.getFirebaseProjectId().equals(a.getProjectId())
				|| !binding.getFirebaseUid().equals(a.getFirebaseUid())) conflict();
		if (a.getCleanupAt() != null && !clock.instant().isBefore(a.getCleanupAt())) throw error(AuthErrorStatus.PROVIDER_OPERATION_NOT_FOUND);
	}
	private Status view(ProviderLinkAttempt a, boolean allowed) {
		boolean expired = !clock.instant().isBefore(a.getExpiresAt());
		boolean stale = security.captureEpoch(a.getUserId()) != a.getEpoch()
				|| changes.captureRevision(a.getUserId()) != a.getRevision() + (a.getState() == ProviderLinkAttempt.State.COMPLETED ? 1 : 0);
		String state = a.getState().name();
		if (a.getState() == ProviderLinkAttempt.State.STARTED && (expired || stale)) state = "ACTION_REQUIRED";
		else if (stale) state = "SUPERSEDED";
		else if (a.getState() == ProviderLinkAttempt.State.PREPARED && expired) state = "EXPIRED";
		return new Status(a.getAttemptId(), a.getProvider(), state, a.getExpiresAt(), allowed);
	}
}
