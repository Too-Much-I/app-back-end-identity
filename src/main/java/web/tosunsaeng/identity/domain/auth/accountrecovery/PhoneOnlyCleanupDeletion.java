package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Clock;
import java.util.Objects;
import com.google.firebase.auth.*;
import static web.tosunsaeng.identity.domain.auth.accountrecovery.PhoneOnlyCleanupPolicy.*;

/** A failed/unknown mutation is fenced for inspection, never retried as a fresh candidate. */
public final class PhoneOnlyCleanupDeletion {
    public enum Outcome { SKIPPED, DELETED, ABSENT, BLOCKED }
    private final AbstractFirebaseAuth firebase;
    private final PhoneOnlyCleanupFence fence;
    private final PhoneOnlyCleanupAudit audit;
    private final PhoneOnlyCleanupPolicy policy = new PhoneOnlyCleanupPolicy();
    private final String project;
    private final Clock clock;

    public PhoneOnlyCleanupDeletion(AbstractFirebaseAuth firebase, PhoneOnlyCleanupFence fence,
                                    PhoneOnlyCleanupAudit audit, String project, Clock clock) {
        this.firebase = firebase; this.fence = fence; this.audit = audit; this.project = project; this.clock = clock;
    }

    public Outcome process(String uid) {
        boolean claimed = false;
        try {
            UserRecord before = firebase.getUser(uid);
            if (!eligible(before, false)) return Outcome.SKIPPED;
            if (!fence.claim(project, uid, () -> eligible(before, false))) return Outcome.SKIPPED;
            claimed = true;
            UserRecord current = firebase.getUser(uid);
            if (!sameAccount(before, current) || !eligible(current, false)) {
                fence.mark(project, uid, "CHANGED_BEFORE_DISABLE", false);
                return Outcome.BLOCKED;
            }
            // Disable and revoke before the final snapshot; never delete an enabled UID blindly.
            firebase.updateUser(new UserRecord.UpdateRequest(uid).setDisabled(true));
            firebase.revokeRefreshTokens(uid);
            current = firebase.getUser(uid);
            if (!current.isDisabled() || !sameAccount(before, current) || !eligible(current, true)) {
                fence.mark(project, uid, "CHANGED_AFTER_DISABLE", false);
                return Outcome.BLOCKED;
            }
            firebase.deleteUser(uid);
            try {
                firebase.getUser(uid);
                fence.mark(project, uid, "DELETE_NOT_CONFIRMED", false);
                return Outcome.BLOCKED;
            } catch (FirebaseAuthException absent) {
                if (absent.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND) throw absent;
            }
            fence.mark(project, uid, "DELETED", true);
            return Outcome.DELETED;
        } catch (FirebaseAuthException failure) {
            if (failure.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
                if (claimed) safeMark(uid, "ABSENT", true);
                return Outcome.ABSENT;
            }
            if (claimed) safeMark(uid, "RESULT_UNKNOWN", false);
            return Outcome.BLOCKED;
        } catch (RuntimeException failure) {
            if (claimed) safeMark(uid, "RESULT_UNKNOWN", false);
            return Outcome.BLOCKED;
        }
    }

    private void safeMark(String uid, String outcome, boolean terminal) {
        try { fence.mark(project, uid, outcome, terminal); } catch (RuntimeException ignored) {
            // Existing claim remains blocked. Do not log identifiers or exception bodies.
        }
    }

    private boolean eligible(UserRecord user, boolean disabledByUs) {
        Snapshot s = PhoneOnlyCleanupAudit.snapshot(user);
        if (disabledByUs) s = new Snapshot(s.providerIds(), s.hasPhone(), s.hasEmail(), s.hasCustomClaims(),
                false, s.createdAt(), s.signedInAt(), s.refreshedAt(), s.recoveryActivityAt());
        return policy.evaluate(s, audit.state(user), clock.instant()) == Decision.CANDIDATE;
    }

    private static boolean sameAccount(UserRecord a, UserRecord b) {
        return Objects.equals(a.getUid(), b.getUid()) && Objects.equals(a.getPhoneNumber(), b.getPhoneNumber())
                && a.getUserMetadata() != null && b.getUserMetadata() != null
                && a.getUserMetadata().getCreationTimestamp() == b.getUserMetadata().getCreationTimestamp();
    }
}
