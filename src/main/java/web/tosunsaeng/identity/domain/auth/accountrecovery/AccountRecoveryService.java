package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.federation.application.*;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;

public final class AccountRecoveryService {
	public record Prepared(String recoveryId, Instant expiresAt) { }
	private final RecoveryStore store;
	private final RecoveryHasher hasher;
	private final FirebaseAuthenticationVerifier verifier;
	private final RecoveryAccountResolver resolver;
	private final RecoveryProperties properties;
	private final Clock clock;
	private final String tenantId;
	public AccountRecoveryService(RecoveryStore store, RecoveryHasher hasher, FirebaseAuthenticationVerifier verifier,
			RecoveryAccountResolver resolver, RecoveryProperties properties, Clock clock) {
		this(store, hasher, verifier, resolver, properties, clock, null);
	}
	public AccountRecoveryService(RecoveryStore store, RecoveryHasher hasher, FirebaseAuthenticationVerifier verifier,
			RecoveryAccountResolver resolver, RecoveryProperties properties, Clock clock, String tenantId) {
		this.store=store; this.hasher=hasher; this.verifier=verifier; this.resolver=resolver; this.properties=properties; this.clock=clock;
		this.tenantId = tenantId == null ? "" : tenantId;
	}
	public Prepared prepare(String address) {
		try {
			Instant now = clock.instant();
			store.admit(hasher.hashes("prepare-ip", address), 60, properties.preparePerMinute(), now);
			String id = UUID.randomUUID().toString();
			Instant expiresAt = now.plus(properties.challengeTtl());
			store.prepare(id, now, expiresAt);
			return new Prepared(id, expiresAt);
		} catch (DataAccessException | org.springframework.transaction.TransactionException e) {
			throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		}
	}
	public RecoveryResult lookup(String id, String token, String address) {
		try {
			Instant now = clock.instant();
			store.admit(hasher.hashes("lookup-ip", address), 60, properties.lookupPerMinute(), now);
			try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
			catch (RuntimeException e) { throw new AuthException(AuthErrorStatus.INVALID_RECOVERY_REQUEST); }
			VerifiedFirebasePrincipal proof = verify(token);
			if (proof.signInMethod() != FirebaseAuthenticationMethod.PHONE || !proof.phoneVerified()
					|| proof.verifiedPhoneNumber() == null || !proof.expiresAt().isAfter(now)
					|| proof.issuedAt().isAfter(now.plusSeconds(30)) || proof.authTime().isAfter(now.plusSeconds(30))) {
				throw new AuthException(AuthErrorStatus.INVALID_RECOVERY_PROOF);
			}
			if (proof.authTime().isBefore(now.minus(properties.recentAuth()))) throw new AuthException(AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED);
			String phone;
			try { phone = new PhoneNumberNormalizer().normalize(proof.verifiedPhoneNumber()); }
			catch (RuntimeException e) { throw new AuthException(AuthErrorStatus.INVALID_RECOVERY_PROOF); }
			store.admit(hasher.hashes("lookup-uid", proof.firebaseProjectId(), tenantId, proof.firebaseUid()), 900, properties.proofPerQuarterHour(), now);
			store.admit(hasher.hashes("lookup-phone", phone), 900, properties.proofPerQuarterHour(), now);
			var proofIds = hasher.hashes("proof", proof.firebaseProjectId(), tenantId, proof.firebaseUid(), proof.authTime().toString(), phone);
			return store.lookup(id, proofIds, proof.authTime(), now, properties.retryTtl(), () -> resolver.resolve(phone));
		} catch (DataAccessException | org.springframework.transaction.TransactionException e) {
			throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		}
	}
	private VerifiedFirebasePrincipal verify(String token) {
		try { return verifier.verify(token, FirebaseVerificationPurpose.ACCOUNT_RECOVERY); }
		catch (AuthException e) {
			if (e.getErrorCode() == AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED) throw new AuthException(AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED);
			if (e.getErrorCode() == AuthErrorStatus.FIREBASE_UNAVAILABLE) throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
			if (e.getErrorCode() == AuthErrorStatus.FIREBASE_RATE_LIMITED) throw new AuthException(AuthErrorStatus.RECOVERY_RATE_LIMITED);
			throw new AuthException(AuthErrorStatus.INVALID_RECOVERY_PROOF);
		}
	}
}
