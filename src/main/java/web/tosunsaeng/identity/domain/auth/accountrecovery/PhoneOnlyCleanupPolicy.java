package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

/** Candidate selection only: this decision is never an authorization to delete a Firebase UID. */
public final class PhoneOnlyCleanupPolicy {
    public static final Duration PROTECTION = Duration.ofMinutes(30);
    public static final Duration SCAN_INTERVAL = Duration.ofMinutes(5);

    public enum Decision { CANDIDATE, NOT_PHONE_ONLY, PROTECTED, RECENT, UNKNOWN }

    public record Snapshot(Set<String> providerIds, boolean hasPhone, boolean hasEmail,
                           boolean hasCustomClaims, boolean disabled,
                           Instant createdAt, Instant signedInAt, Instant refreshedAt,
                           Instant recoveryActivityAt) {
        public Snapshot {
            providerIds = providerIds == null ? null : Set.copyOf(providerIds);
        }
    }

    /** False for unknown query results; all checks use the production Identity database. */
    public record ProductionState(boolean checksComplete, boolean hasMemberBinding,
                                  boolean hasEnrollment, boolean hasPhoneOwnership,
                                  boolean hasProtection) { }

    public Decision evaluate(Snapshot snapshot, ProductionState state, Instant now) {
        if (snapshot == null || state == null || now == null || !state.checksComplete()) {
            return Decision.UNKNOWN;
        }
        if (snapshot.providerIds() == null) return Decision.UNKNOWN;
        // Unknown providers are not treated as absent social providers.
        if (!snapshot.hasPhone() || !snapshot.providerIds().equals(Set.of("phone"))
                || snapshot.hasEmail() || snapshot.hasCustomClaims()) {
            return Decision.NOT_PHONE_ONLY;
        }
        if (snapshot.disabled() || state.hasMemberBinding() || state.hasEnrollment()
                || state.hasPhoneOwnership() || state.hasProtection()) {
            return Decision.PROTECTED;
        }
        if (snapshot.createdAt() == null || snapshot.signedInAt() == null) return Decision.UNKNOWN;
        Instant latest = snapshot.createdAt();
        Instant[] activity = { snapshot.signedInAt(), snapshot.refreshedAt(), snapshot.recoveryActivityAt() };
        for (Instant time : activity) {
            if (time != null && time.isAfter(latest)) latest = time;
        }
        if (latest.isAfter(now)) return Decision.UNKNOWN;
        return Duration.between(latest, now).compareTo(PROTECTION) >= 0
                ? Decision.CANDIDATE : Decision.RECENT;
    }
}
