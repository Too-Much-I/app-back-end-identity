package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import com.mongodb.MongoException;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.session.infrastructure.ReissueRecoveryProperties;
import web.tosunsaeng.identity.domain.auth.session.repository.*;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

/** HTTP response loss and Mongo commit uncertainty are distinct: uncertain commits never mint again. */
public final class ReissueRecoveryService {
	private final RefreshSessionRepository sessions;
	private final RefreshReissueResponseRepository responses;
	private final UserRepository users;
	private final RefreshTokenHasher hasher;
	private final RefreshSessionIssuer refreshIssuer;
	private final AccessTokenIssuer accessIssuer;
	private final AuthResponseConverter converter;
	private final SessionSecurityService security;
	private final TransactionTemplate transactions;
	private final ReissueResponseCipher cipher;
	private final ReissueRecoveryProperties properties;
	private final Clock clock;
	private final MeterRegistry metrics;

	public ReissueRecoveryService(RefreshSessionRepository sessions, RefreshReissueResponseRepository responses,
			UserRepository users, RefreshTokenHasher hasher, RefreshSessionIssuer refreshIssuer, AccessTokenIssuer accessIssuer,
			AuthResponseConverter converter, SessionSecurityService security, TransactionTemplate transactions,
			ReissueResponseCipher cipher, ReissueRecoveryProperties properties, Clock clock, MeterRegistry metrics) {
		this.sessions = sessions; this.responses = responses; this.users = users; this.hasher = hasher;
		this.refreshIssuer = refreshIssuer; this.accessIssuer = accessIssuer; this.converter = converter;
		this.security = security; this.transactions = transactions; this.cipher = cipher;
		this.properties = properties; this.clock = clock; this.metrics = metrics;
	}
	public ReissueResult reissue(ReissueRequest request, List<String> requestIds) {
		String requestHash = requestHash(requestIds);
		if (properties.isMaintenance()) throw SessionSecurityService.unavailable();
		String sourceHash = hasher.hash(request.refreshToken());
		boolean replayOnly = false;
		for (int attempt = 0; attempt < properties.getConcurrencyAttempts(); attempt++) {
			final boolean only = replayOnly;
			try {
				Outcome outcome = transactions.execute(status -> rotateOrReplay(sourceHash, requestHash, only));
				if (outcome == null) throw SessionSecurityService.unavailable();
				count(outcome.event());
				if (outcome.error() != null) throw outcome.error(); // security revocations have committed
				ensureDeadline(outcome.result());
				return outcome.result();
			} catch (DataAccessException | TransactionException | MongoException exception) {
				if (label(exception, "UnknownTransactionCommitResult")) replayOnly = true;
				else if (!retryable(exception)) throw unavailable("transaction_unresolved");
				count("transaction_retry");
				if (attempt + 1 < properties.getConcurrencyAttempts()) pause();
			}
		}
		throw unavailable("transaction_unresolved");
	}
	private Outcome rotateOrReplay(String sourceHash, String requestHash, boolean replayOnly) {
		RefreshSession source = sessions.findByTokenHash(sourceHash).orElseThrow(() -> error(AuthErrorStatus.INVALID_REFRESH_TOKEN));
		if (source.getRevocationReason() == RevocationReason.ACCOUNT_WITHDRAWN) throw error(AuthErrorStatus.ACCOUNT_WITHDRAWN);
		security.checkAndTouch(source, false);
		Instant now = clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
		if (source.isExpiredAt(now)) throw error(AuthErrorStatus.REFRESH_TOKEN_EXPIRED);
		var user = users.findById(source.getUserId()).orElseThrow(() -> error(AuthErrorStatus.INVALID_REFRESH_TOKEN));
		var type = user.getAccountType();
		if (type == null) throw SessionSecurityService.unavailable();
		if (source.getRevocationReason() == RevocationReason.ROTATED) {
			if (source.getRecoveryDisabledAt() != null) throw error(AuthErrorStatus.SESSION_LOGGED_OUT);
			if (requestHash.equals(source.getRotationRequestKeyHash())) {
				if (source.getRecoveryUntil() == null) throw SessionSecurityService.unavailable();
				if (!now.isBefore(source.getRecoveryUntil())) return rejected(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED, "recovery_expired");
				if (type != source.getIssuedAccountType()) return rejected(AuthErrorStatus.REISSUE_RESULT_SUPERSEDED, "superseded");
				return replay(source, now);
			}
			if (replayOnly) throw SessionSecurityService.unavailable();
			var active = sessions.findAllByUserIdAndRevokedAtIsNull(source.getUserId()).stream().filter(s -> !s.isRevoked()).toList();
			active.forEach(s -> s.revokeForReuse(now));
			if (!active.isEmpty()) sessions.saveAll(active);
			return rejected(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED, "suspected_reuse");
		}
		if (source.isRevoked()) throw error(AuthErrorStatus.SESSION_LOGGED_OUT);
		if (replayOnly) throw SessionSecurityService.unavailable();
		if (sessions.existsByUserIdAndRotationRequestKeyHash(source.getUserId(), requestHash)) {
			return rejected(AuthErrorStatus.REISSUE_REQUEST_CONFLICT, "id_conflict");
		}
		source.initializeRotationFamilyIfMissing();
		String childId = RefreshSession.newSessionId();
		source.rotate(now, childId);
		Instant until = now.plus(properties.getWindow()).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
		if (source.getExpiresAt().isBefore(until)) until = source.getExpiresAt();
		source.recordRecovery(requestHash, UUID.randomUUID().toString(), now, until, type);
		sessions.save(source);
		var access = accessIssuer.issue(source.getUserId(), type, Set.of());
		var refresh = refreshIssuer.issueRotated(source, childId, now);
		Instant accessExpiry;
		try { accessExpiry = SignedJWT.parse(access.tokenValue()).getJWTClaimsSet().getExpirationTime().toInstant(); }
		catch (Exception exception) { throw SessionSecurityService.unavailable(); }
		var result = new ReissueResult(converter.toReissueResponse(access, refresh.tokenValue(), refresh.expiresAt(), now),
				accessExpiry, refresh.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MILLIS), source.getRecoveryUntil());
		ensureDeadline(result);
		try { responses.insert(cipher.encrypt(source, result)); }
		catch (AuthException exception) { throw unavailable("crypto_failure"); }
		return new Outcome(result, null, "first_issue");
	}
	private Outcome replay(RefreshSession source, Instant now) {
		RefreshSession child = sessions.findById(source.getReplacedBySessionId()).orElseThrow(SessionSecurityService::unavailable);
		validateChild(source, child);
		if (child.getRevocationReason() == RevocationReason.ROTATED) return rejected(AuthErrorStatus.REISSUE_RESULT_SUPERSEDED, "superseded");
		if (child.isRevoked() || child.isExpiredAt(now)) throw error(AuthErrorStatus.SESSION_LOGGED_OUT);
		security.checkAndTouch(child, false);
		child.touchForRecovery(now);
		sessions.save(child); // CAS write, not just a snapshot read; conflicts with logout/rotation
		ReissueResult result;
		try {
			var document = responses.findById(source.getRotationResponseId()).orElseThrow(SessionSecurityService::unavailable);
			result = cipher.decrypt(source, document);
			var response = Objects.requireNonNull(result.response());
			var claims = SignedJWT.parse(response.accessToken()).getJWTClaimsSet();
			// Ciphertext authenticity is already verified. This is consistency checking, not external JWT authentication.
			if (!hasher.hash(response.refreshToken()).equals(child.getTokenHash())
					|| !child.getExpiresAt().equals(result.refreshExpiresAt())
					|| !source.getUserId().equals(claims.getSubject())
					|| !source.getIssuedAccountType().name().equals(claims.getStringClaim("account_type"))
					|| !result.accessExpiresAt().equals(claims.getExpirationTime().toInstant())
					|| !"Bearer".equals(response.grantType()) || response.accessTokenExpiresIn() <= 0 || response.refreshTokenExpiresIn() <= 0) {
				throw SessionSecurityService.unavailable();
			}
		} catch (Exception exception) { throw unavailable("crypto_failure"); }
		ensureDeadline(result);
		return new Outcome(result, null, "replayed");
	}
	private void validateChild(RefreshSession source, RefreshSession child) {
		if (!source.getUserId().equals(child.getUserId()) || !source.getSessionId().equals(child.getRotatedFromSessionId())
				|| !source.getRotationFamilyId().equals(child.getRotationFamilyId()) || source.getSessionEpoch() != child.getSessionEpoch()
				|| !Objects.equals(source.getAuthentication(), child.getAuthentication())) throw SessionSecurityService.unavailable();
	}
	/** Both normal child logout and lost-response source cancellation run in the same writer boundary. */
	public void logout(String rawRefresh) {
		String hash = hasher.hash(rawRefresh);
		runLogoutTransaction(() -> {
			var source = sessions.findByTokenHash(hash).orElse(null);
			Instant now = clock.instant();
			if (source == null || source.isExpiredAt(now)) return null;
			if (!source.isRevoked()) { source.logout(now); sessions.save(source); return null; }
			if (source.getRevocationReason() != RevocationReason.ROTATED || source.getRecoveryUntil() == null
					|| !now.isBefore(source.getRecoveryUntil()) || source.getRecoveryDisabledAt() != null) return null;
			// Logout remains idempotent for inactive users/old epochs; it cannot mint or traverse descendants.
			var control = security.control(source.getUserId());
			security.touchExpected(source.getUserId(), control.getSessionEpoch());
			source.cancelRecovery(now); sessions.save(source);
			var child = sessions.findById(source.getReplacedBySessionId()).orElseThrow(SessionSecurityService::unavailable);
			validateChild(source, child);
			if (!child.isRevoked() && !child.isExpiredAt(now)) { child.logout(now); sessions.save(child); }
			return null;
		});
	}
	private void runLogoutTransaction(Supplier<Void> work) {
		for (int attempt = 0; attempt < properties.getConcurrencyAttempts(); attempt++) {
			try { transactions.execute(status -> work.get()); return; }
			catch (DataAccessException | TransactionException | MongoException exception) {
				// Repeating exact idempotent cancellation never creates another credential.
				if (!label(exception, "UnknownTransactionCommitResult") && !retryable(exception)) throw SessionSecurityService.unavailable();
				if (attempt + 1 < properties.getConcurrencyAttempts()) pause();
			}
		}
		throw SessionSecurityService.unavailable();
	}
	private String requestHash(List<String> values) {
		if (values == null || values.size() != 1 || values.get(0) == null
				|| !values.get(0).matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
			throw error(AuthErrorStatus.INVALID_REISSUE_REQUEST_ID);
		}
		return hasher.hash(values.get(0));
	}
	private void ensureDeadline(ReissueResult result) {
		Instant now = clock.instant();
		if (!now.isBefore(result.recoveryUntil()) || !now.isBefore(result.accessExpiresAt()) || !now.isBefore(result.refreshExpiresAt())) {
			throw error(AuthErrorStatus.REISSUE_RECOVERY_EXPIRED);
		}
	}
	private boolean retryable(Throwable exception) {
		if (label(exception, "UnknownTransactionCommitResult")) return false;
		return exception instanceof DuplicateKeyException || exception instanceof OptimisticLockingFailureException
				|| label(exception, "TransientTransactionError");
	}
	private boolean label(Throwable exception, String label) {
		for (int i = 0; exception != null && i < 16; i++, exception = exception.getCause()) {
			if (exception instanceof MongoException mongo && mongo.hasErrorLabel(label)) return true;
		}
		return false;
	}
	private void pause() {
		try { Thread.sleep(ThreadLocalRandom.current().nextLong(10, 51)); }
		catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw SessionSecurityService.unavailable(); }
	}
	private void count(String event) { metrics.counter("auth.reissue.recovery", "outcome", event).increment(); }
	private AuthException unavailable(String event) { count(event); return SessionSecurityService.unavailable(); }
	private AuthException error(AuthErrorStatus status) { return new AuthException(status); }
	private Outcome rejected(AuthErrorStatus status, String event) { return new Outcome(null, error(status), event); }
	private record Outcome(ReissueResult result, AuthException error, String event) {
		@Override public String toString() { return "Outcome[redacted]"; }
	}
}
