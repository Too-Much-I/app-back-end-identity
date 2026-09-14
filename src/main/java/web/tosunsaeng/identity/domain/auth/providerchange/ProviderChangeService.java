package web.tosunsaeng.identity.domain.auth.providerchange;

import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard.error;
import static web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard.social;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseVerificationPurpose;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

public class ProviderChangeService {
	private ProviderChangeMetrics metrics;
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setMetrics(ProviderChangeMetrics metrics) { this.metrics = metrics; }
	private void record(ProviderChangeMetrics.Outcome outcome) { if (metrics != null) metrics.record(outcome); }
	public record Status(String operationId, SocialProvider provider, String status, Instant acceptedAt,
			Instant completedAt, Integer nextPollAfterSeconds) { }
	public record Permit(String linkAttemptId, Instant expiresAt) { }
	private final MongoTemplate mongo;
	private final TransactionTemplate transactions;
	private final SessionSecurityService security;
	private final ProviderChangeGuard guard;
	private final FirebaseAuthenticationVerifier verifier;
	private final FirebaseIdentityRepository identities;
	private final SocialIdentityRepository socials;
	private final UserRepository users;
	private final ProviderChangeProperties properties;
	private final Clock clock;
	private final String tenant;
	private final ProviderRequestBudget ownerBudget = new ProviderRequestBudget(10_000, 60);
	void rateLimit(String userId) {
		if (!ownerBudget.admit(userId, clock.instant().getEpochSecond() / 60)) throw error(AuthErrorStatus.PROVIDER_RATE_LIMITED);
	}

	public ProviderChangeService(MongoTemplate mongo, TransactionTemplate transactions, SessionSecurityService security,
			ProviderChangeGuard guard, FirebaseAuthenticationVerifier verifier, FirebaseIdentityRepository identities,
			SocialIdentityRepository socials, UserRepository users, ProviderChangeProperties properties, Clock clock, String tenant) {
		this.mongo = mongo; this.transactions = transactions; this.security = security; this.guard = guard;
		this.verifier = verifier; this.identities = identities; this.socials = socials; this.users = users;
		this.properties = properties; this.clock = clock;
		this.tenant = tenant;
	}

	public Status unlink(String userId, SocialProvider provider, String token, List<String> keys) {
		if (!properties.isEnabled()) throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		String hash = requestHash(keys);
		Objects.requireNonNull(provider);
		long epoch = security.captureEpoch(userId);
		long revision = captureRevision(userId);
		var proof = verify(token);
		FirebaseIdentity snapshot = binding(userId, proof);
		rateLimit(userId);
		return transaction(() -> {
			FirebaseIdentity binding = binding(userId, proof);
			exactBinding(snapshot, binding);
			var existing = mongo.findOne(Query.query(Criteria.where("userId").is(userId)
					.and("requestIdHash").is(hash)), ProviderUnlinkOperation.class);
			if (existing != null) {
				if (provider != existing.getProvider() || !binding.getFirebaseIdentityId().equals(existing.getBindingId())) conflict();
				remainingProof(existing, proof); record(ProviderChangeMetrics.Outcome.DUPLICATE); return view(existing);
			}
			var control = security.control(userId); control.requireEpoch(epoch);
			var methodControl = guard.control(userId, binding.getFirebaseIdentityId());
			if (methodControl.getRevision() != revision) conflict();
			if (control.getActiveLogoutId() != null || security.hasUnresolvedLogout(userId)) conflict();
			var remaining = remaining(userId, proof, provider, methodControl);
			if (remaining.isEmpty()) {
				if (metrics != null) metrics.rejected(ProviderChangeMetrics.Outcome.LAST_METHOD);
				throw error(AuthErrorStatus.PROVIDER_LAST_METHOD);
			}
			if (!remaining.contains(social(proof.signInMethod()))) throw error(AuthErrorStatus.PROVIDER_REMAINING_AUTH_REQUIRED);
			guard.authenticate(userId, binding.getFirebaseIdentityId(), proof.signInMethod(), proof.authTime());
			security.checkFirebaseAuthentication(userId, security.firebase(userId, epoch, proof));
			// Reload after the common security writer changed its optimistic version.
			control = security.control(userId);
			SocialIdentity target = ownedSocial(userId, provider, proof);
			if (methodControl.isBlocked(provider)) throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
			Instant now = clock.instant();
			control.logout(binding.getFirebaseIdentityId(), now);
			var op = ProviderUnlinkOperation.create(hash, binding, provider, target.getSocialIdentityId(), remaining, now, control.getSessionEpoch(), tenant);
			control.claimLogout(op.slot()); methodControl.block(provider, now);
			mongo.save(control); mongo.save(methodControl); mongo.insert(op);
			record(ProviderChangeMetrics.Outcome.ACCEPTED);
			return view(op);
		});
	}

	/** Fresh Firebase ownership proof, never the request ID alone, authorizes read-only status. */
	public Status status(String requestId, String token) {
		String hash = requestHash(java.util.Collections.singletonList(requestId));
		var proof = verify(token);
		var binding = binding(null, proof);
		rateLimit(binding.getUserId());
		return transaction(() -> {
			exactBinding(binding, binding(binding.getUserId(), proof));
			var op = mongo.findOne(Query.query(Criteria.where("userId").is(binding.getUserId())
					.and("requestIdHash").is(hash)), ProviderUnlinkOperation.class);
			if (op == null || !binding.getFirebaseIdentityId().equals(op.getBindingId())
					|| (op.getCleanupAt() != null && !clock.instant().isBefore(op.getCleanupAt()))) {
				throw error(AuthErrorStatus.PROVIDER_OPERATION_NOT_FOUND);
			}
			remainingProof(op, proof);
			guard.authenticate(binding.getUserId(), binding.getFirebaseIdentityId(), proof.signInMethod(), proof.authTime());
			var c = security.control(binding.getUserId());
			c.validate(c.getSessionEpoch(), security.firebase(binding.getUserId(), c.getSessionEpoch(), proof), true);
			return view(op);
		});
	}

	/** Retired protocol: legacy attempts are retained as unresolved, never upgraded implicitly. */
	@Deprecated
	public Permit prepare(String userId, SocialProvider provider, String token) {
		throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
	}

	public long captureRevision(String userId) {
		try {
			var control = mongo.findById(userId, AuthMethodChangeControl.class);
			return control == null ? 0 : control.getRevision();
		} catch (DataAccessException exception) { throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE); }
	}

	/** Legacy sync is validation-only; no grant, save, or unblock is accepted here. */
	public void synchronize(String userId, long epoch, long expectedRevision, VerifiedFirebasePrincipal proof, String permitId, Runnable ignored) {
		if (permitId != null) throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
		var binding = binding(userId, proof);
		rateLimit(userId);
		var control = security.control(userId); control.requireEpoch(epoch);
		var methods = guard.control(userId, binding.getFirebaseIdentityId());
		if (methods.getRevision() != expectedRevision || control.getActiveLogoutId() != null) conflict();
		for (var p : proof.linkedSocialPrincipals()) {
			if (methods.isBlocked(p.provider())) throw error(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
			ownedSocial(userId, p.provider(), proof);
		}
		security.checkFirebaseAuthentication(userId, security.firebase(userId, epoch, proof));
	}

	VerifiedFirebasePrincipal verify(String token) {
		var proof = verifier.verify(token, FirebaseVerificationPurpose.HIGH_RISK_REAUTHENTICATION);
		recent(proof); return proof;
	}
	private void recent(VerifiedFirebasePrincipal proof) {
		Instant now = clock.instant();
		if (social(proof.signInMethod()) == null) throw error(AuthErrorStatus.PROVIDER_REMAINING_AUTH_REQUIRED);
		if (proof.authTime().isAfter(now) || proof.authTime().isBefore(now.minus(properties.getRecentAuth()))
				|| !now.isBefore(proof.expiresAt())) throw error(AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
	}
	FirebaseIdentity binding(String expectedUserId, VerifiedFirebasePrincipal proof) {
		var binding = identities.findByFirebaseProjectIdAndFirebaseUid(proof.firebaseProjectId(), proof.firebaseUid())
				.orElseThrow(() -> error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		if (expectedUserId != null && !expectedUserId.equals(binding.getUserId())) conflict();
		security.requireActive(binding.getUserId());
		if (!users.findById(binding.getUserId()).orElseThrow(() -> error(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT)).isMember()) {
			throw error(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
		}
		return binding;
	}
	Set<SocialProvider> remaining(String userId, VerifiedFirebasePrincipal proof, SocialProvider target, AuthMethodChangeControl control) {
		Set<SocialProvider> result = EnumSet.noneOf(SocialProvider.class);
		for (var p : proof.linkedSocialPrincipals()) if (p.provider() != target && !control.isBlocked(p.provider())) {
			ownedSocial(userId, p.provider(), proof); result.add(p.provider());
		}
		return result;
	}
	SocialIdentity ownedSocial(String userId, SocialProvider provider, VerifiedFirebasePrincipal proof) {
		var remote = proof.linkedSocialPrincipals().stream().filter(p -> p.provider() == provider).findFirst()
				.orElseThrow(() -> error(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT));
		var identity = socials.findByProviderAndProviderSubject(provider, remote.providerSubject())
				.orElseThrow(() -> error(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT));
		if (!userId.equals(identity.getUserId())) throw error(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
		return identity;
	}
	private void remainingProof(ProviderUnlinkOperation op, VerifiedFirebasePrincipal proof) {
		if (!op.getRemainingProviders().contains(social(proof.signInMethod()))) throw error(AuthErrorStatus.PROVIDER_REMAINING_AUTH_REQUIRED);
		ownedSocial(op.getUserId(), social(proof.signInMethod()), proof);
	}
	static void exactBinding(FirebaseIdentity a, FirebaseIdentity b) {
		if (!a.getFirebaseIdentityId().equals(b.getFirebaseIdentityId()) || !a.getCreatedAt().equals(b.getCreatedAt())) conflict();
	}
	<T> T transaction(Supplier<T> task) {
		try { return transactions.execute(status -> task.get()); }
		catch (DataAccessException | TransactionException exception) {
			// No blind remint on ambiguous commit. Caller recovers via status using the original request ID.
			throw error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		}
	}
	static void conflict() { throw error(AuthErrorStatus.PROVIDER_CHANGE_CONFLICT); }
	static String requestHash(List<String> keys) {
		if (keys == null || keys.size() != 1) throw error(AuthErrorStatus.INVALID_PROVIDER_REQUEST_ID);
		String key = uuid(keys.get(0));
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8))); }
		catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable."); }
	}
	static String uuid(String value) {
		try {
			UUID parsed = UUID.fromString(value);
			if (parsed.version() != 4 || parsed.variant() != 2 || !parsed.toString().equals(value)) throw new IllegalArgumentException();
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) { throw error(AuthErrorStatus.INVALID_PROVIDER_REQUEST_ID); }
	}
	static Status view(ProviderUnlinkOperation op) {
		String state = switch (op.getState()) {
			case COMPLETED -> "COMPLETED"; case SUPERSEDED -> "SUPERSEDED";
			case RECONCILIATION_REQUIRED -> "ACTION_REQUIRED"; default -> "PROCESSING";
		};
		return new Status(op.getOperationId(), op.getProvider(), state, op.getAcceptedAt(), op.getCompletedAt(),
				op.terminal() || op.getState() == ProviderUnlinkOperation.State.RECONCILIATION_REQUIRED ? null : 3);
	}
}
