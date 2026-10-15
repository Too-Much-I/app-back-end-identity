package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import static web.tosunsaeng.identity.domain.auth.accountrecovery.PhoneOnlyCleanupPolicy.*;

/** Bounded scan. Read-only unless an explicitly configured deletion worker is attached. */
public final class PhoneOnlyCleanupAudit {
    private final AbstractFirebaseAuth firebase;
    private final MongoTemplate mongo;
    private final PhoneFingerprintHasher hasher;
    private final Clock clock;
    private final PhoneOnlyCleanupPolicy policy = new PhoneOnlyCleanupPolicy();
    private String pageToken;
    private PhoneOnlyCleanupDeletion deletion;

    void setDeletion(PhoneOnlyCleanupDeletion deletion) { this.deletion = deletion; }

    public PhoneOnlyCleanupAudit(AbstractFirebaseAuth firebase, MongoTemplate mongo,
                                 PhoneFingerprintHasher hasher, Clock clock) {
        this.firebase = firebase; this.mongo = mongo; this.hasher = hasher; this.clock = clock;
    }

    public record Report(int scanned, boolean sweepComplete, java.util.Map<Decision, Integer> counts,
                         java.util.Map<PhoneOnlyCleanupDeletion.Outcome, Integer> deletions) { }

    public synchronized Report inspectPage() throws com.google.firebase.auth.FirebaseAuthException {
        // Bounded page, no export of phone numbers, UIDs, claims or credentials.
        var page = firebase.listUsers(pageToken, 100);
        var counts = new EnumMap<Decision, Integer>(Decision.class);
        int scanned = 0;
        var outcomes = new EnumMap<PhoneOnlyCleanupDeletion.Outcome, Integer>(PhoneOnlyCleanupDeletion.Outcome.class);
        for (var listed : page.getValues()) {
            scanned++;
            Decision result;
            try {
                var user = firebase.getUser(listed.getUid());
                var snapshot = snapshot(user);
                var prelim = policy.evaluate(snapshot,
                        new ProductionState(true, false, false, false, false), clock.instant());
                result = prelim == Decision.CANDIDATE
                        ? policy.evaluate(snapshot, state(user), clock.instant()) : prelim;
                if (result == Decision.CANDIDATE && deletion != null) {
                    outcomes.merge(deletion.process(user.getUid()), 1, Integer::sum);
                }
            } catch (Exception failure) {
                // Do not log exception messages: SDK/database exceptions may contain identifiers.
                result = Decision.UNKNOWN;
            }
            counts.merge(result, 1, Integer::sum);
        }
        pageToken = page.hasNextPage() ? page.getNextPageToken() : null;
        return new Report(scanned, pageToken == null, java.util.Map.copyOf(counts), java.util.Map.copyOf(outcomes));
    }

    static Snapshot snapshot(UserRecord user) {
        var metadata = user.getUserMetadata();
        java.util.Set<String> providers = null;
        if (user.getProviderData() != null) {
            providers = new HashSet<>();
            for (var provider : user.getProviderData()) {
                if (provider == null || provider.getProviderId() == null) { providers = null; break; }
                providers.add(provider.getProviderId());
            }
        }
        return new Snapshot(providers, user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank(),
                user.getEmail() != null && !user.getEmail().isBlank(),
                user.getCustomClaims() != null && !user.getCustomClaims().isEmpty(), user.isDisabled(),
                metadata == null ? null : time(metadata.getCreationTimestamp()),
                metadata == null ? null : time(metadata.getLastSignInTimestamp()),
                metadata == null ? null : time(metadata.getLastRefreshTimestamp()), null);
    }

    ProductionState state(UserRecord user) {
        // Intentionally no status/project filtering: any production reference protects the UID.
        Query uid = Query.query(Criteria.where("firebaseUid").is(user.getUid()));
        boolean binding = mongo.exists(uid, "firebase_identities");
        boolean enrollment = mongo.exists(uid, "firebase_enrollment_attempts");
        boolean protection = false;
        for (String collection : new String[] { "abandoned_firebase_enrollment_cleanups",
                "provider_link_attempts", "provider_unlink_operations", "user_withdrawal_lifecycles",
                "logout_all_operations" }) {
            protection |= mongo.exists(uid, collection);
        }
        boolean phoneOwner = false;
        for (var fingerprint : hasher.fingerprint(user.getPhoneNumber()).retained()) {
            Query phone = Query.query(Criteria.where("fingerprintKeyVersion").is(fingerprint.keyVersion())
                    .and("phoneFingerprint").is(fingerprint.value()));
            phoneOwner |= mongo.exists(phone, "phone_fingerprint_aliases")
                    || mongo.exists(phone, "phone_identities");
        }
        return new ProductionState(true, binding, enrollment, phoneOwner, protection);
    }

    private static Instant time(long millis) { return millis <= 0 ? null : Instant.ofEpochMilli(millis); }
}
