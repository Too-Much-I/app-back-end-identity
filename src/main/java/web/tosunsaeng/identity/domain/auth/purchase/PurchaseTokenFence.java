package web.tosunsaeng.identity.domain.auth.purchase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

/** Synchronous request-scoped bookkeeping only; no credential or token is kept in the context. */
public final class PurchaseTokenFence {
    private final PurchaseProperties properties;
    private final PurchaseReadiness readiness;
    private final PurchaseControlService controls;
    private final TransactionTemplate transactions;
    private final Clock clock;
	private final web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository sessions;
	private final SessionSecurityService security;
    private final ThreadLocal<Deadline> current = new ThreadLocal<>();
    private static final class Deadline { Instant value; }

    public PurchaseTokenFence(PurchaseProperties properties, PurchaseReadiness readiness,
            PurchaseControlService controls, TransactionTemplate transactions, Clock clock,
            web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository sessions,
            SessionSecurityService security) {
        this.properties = Objects.requireNonNull(properties); this.readiness = Objects.requireNonNull(readiness);
        this.controls = Objects.requireNonNull(controls); this.transactions = Objects.requireNonNull(transactions);
        this.clock = Objects.requireNonNull(clock);
		this.sessions = Objects.requireNonNull(sessions); this.security = Objects.requireNonNull(security);
    }

    public <T> T execute(Supplier<T> action) {
        if (current.get() != null) return action.get();
        var deadline = new Deadline();
        current.set(deadline);
        boolean participating = TransactionSynchronizationManager.isActualTransactionActive();
        try {
            T result;
            if (participating) {
                // The outer owner (e.g. recovery) retains its unknown-commit retry semantics.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { checkDeadline(deadline); }
                    @Override public void afterCommit() { checkDeadline(deadline); }
                });
                result = action.get();
            } else {
                try {
                    result = transactions.execute(status -> {
                        T value = action.get();
                        checkDeadline(deadline);
                        return value;
                    });
                } catch (DataAccessException | TransactionException exception) {
                    // No new business retry: commit may have succeeded. No receipt is invented.
                    throw new AuthException(AuthErrorStatus.AUTH_ISSUANCE_UNRESOLVED);
                }
            }
            checkDeadline(deadline);
            return result;
        } finally { current.remove(); }
    }

    public boolean approve(String userId, UserAccountType type, Instant issuedAt, Instant expiresAt) {
        if (current.get() == null) throw SessionSecurityService.unavailable();
        if (!UUID.fromString(userId).toString().equals(userId)) throw SessionSecurityService.unavailable();
        boolean grant = properties.isScopeEnabled() && type == UserAccountType.MEMBER;
        if (grant && (!readiness.ready() || !expiresAt.isAfter(issuedAt)
                || Duration.between(issuedAt, expiresAt).compareTo(Duration.ofMinutes(30)) > 0))
            throw SessionSecurityService.unavailable();
        controls.validateAndTouch(userId, type, grant ? expiresAt : null);
        track(expiresAt);
        return grant;
    }

    public void replay(String userId, UserAccountType type, Instant originalExpiry, boolean purchase) {
        if (current.get() == null) throw SessionSecurityService.unavailable();
        // Disabling new grants cannot bypass the fence on an already-issued purchase response.
        if (purchase && !readiness.ready()) throw SessionSecurityService.unavailable();
        controls.validateAndTouch(userId, type, purchase ? originalExpiry : null);
        track(originalExpiry);
    }
	public void validateSession(web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession expected) {
		PurchaseControlService.requireTransaction();
		var stored = sessions.findById(expected.getSessionId()).orElseThrow(SessionSecurityService::unavailable);
		if (!stored.getUserId().equals(expected.getUserId()) || stored.isRevoked() || stored.isExpiredAt(clock.instant()))
			throw SessionSecurityService.unavailable();
		security.checkAndTouch(stored, false);
	}
    private void track(Instant expiry) {
        var deadline = current.get();
        if (deadline.value == null || expiry.isBefore(deadline.value)) deadline.value = expiry;
    }
    private void checkDeadline(Deadline deadline) {
        if (deadline.value != null && !clock.instant().isBefore(deadline.value))
            throw new AuthException(AuthErrorStatus.AUTH_ISSUANCE_UNRESOLVED);
    }
}
