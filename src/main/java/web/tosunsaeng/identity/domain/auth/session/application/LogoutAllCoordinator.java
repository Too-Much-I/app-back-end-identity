package web.tosunsaeng.identity.domain.auth.session.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.LogoutAllOperation;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.SessionRevocationProperties;

public class LogoutAllCoordinator {
	private final SessionSecurityService security;
	private final MongoTemplate mongo;
	private final FirebaseIdentityRepository identities;
	private final SessionRevocationProperties properties;
	private final String tenantId;
	private final Clock clock;
	public LogoutAllCoordinator(SessionSecurityService security, MongoTemplate mongo,
			FirebaseIdentityRepository identities, SessionRevocationProperties properties, String tenantId, Clock clock) {
		this.security = security; this.mongo = mongo; this.identities = identities;
		this.properties = properties; this.tenantId = tenantId; this.clock = clock;
	}
	public void logoutCurrent(String userId) {
		if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
			throw new AuthException(AuthErrorStatus.INVALID_CREDENTIALS);
		}
		var jwt = token.getToken();
		if (!userId.equals(jwt.getSubject()) || jwt.getIssuer() == null || jwt.getExpiresAt() == null
				|| jwt.getId() == null || !canonicalUuid(jwt.getId())) throw new AuthException(AuthErrorStatus.INVALID_CREDENTIALS);
		accept(userId, fingerprint(jwt.getIssuer().toString(), userId, jwt.getId()), jwt.getExpiresAt());
	}
	public void accept(String userId, String fingerprint, Instant expiresAt) {
		security.transaction(() -> {
			security.requireActive(userId);
			if (mongo.exists(Query.query(Criteria.where("requestFingerprint").is(fingerprint)), LogoutAllOperation.class)) return null;
			var control = security.control(userId);
			var binding = identities.findByUserId(userId).orElse(null);
			Instant now = clock.instant();
			control.logout(binding == null ? null : binding.getFirebaseIdentityId(), now);
			var operation = LogoutAllOperation.create(fingerprint, userId, control.getSessionEpoch(), binding, tenantId, now, expiresAt);
			if (binding == null && security.requiresFirebaseBinding(userId)) operation.unknown("MISSING_FIREBASE_BINDING");
			else if (binding == null) operation.complete(now, cleanupAt(operation, now));
			else if (control.getActiveLogoutId() == null) control.claimLogout(operation.getLogoutId());
			mongo.save(control); mongo.insert(operation);
			// Epoch is the security boundary; bounded physical marking is diagnostic only.
			var sessions = mongo.find(Query.query(Criteria.where("userId").is(userId).and("revokedAt").is(null))
					.limit(1000), RefreshSession.class);
			for (var session : sessions) {
				if (!session.isRevoked()) { session.logoutAll(now); mongo.save(session); }
			}
			return null;
		});
	}
	public Instant cleanupAt(LogoutAllOperation operation, Instant now) {
		Instant retention = now.plus(properties.getRetention());
		Instant request = operation.getRequestExpiresAt().plus(properties.getVerifierSkew());
		return retention.isAfter(request) ? retention : request;
	}
	static String fingerprint(String issuer, String userId, String jti) {
		String material = issuer.length() + ":" + issuer + userId.length() + ":" + userId + jti.length() + ":" + jti;
		try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(material.getBytes(StandardCharsets.UTF_8))); }
		catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 unavailable."); }
	}
	private static boolean canonicalUuid(String value) {
		try { return UUID.fromString(value).toString().equals(value); }
		catch (IllegalArgumentException exception) { return false; }
	}
}
