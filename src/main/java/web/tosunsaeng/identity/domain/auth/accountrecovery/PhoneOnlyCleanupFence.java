package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.HexFormat;
import java.util.function.Supplier;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.exception.*;

/** Shared transaction write orders enrollment creation against deletion claims. */
public final class PhoneOnlyCleanupFence {
    static final String COLLECTION = "phone_only_cleanup_fences";
    private final MongoTemplate mongo;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final String namespace;

    public PhoneOnlyCleanupFence(MongoTemplate mongo, TransactionTemplate tx, Clock clock, String namespace) {
        this.mongo = mongo; this.tx = tx; this.clock = clock; this.namespace = namespace;
    }

    public <T> T enroll(String project, String uid, Supplier<T> action) {
        try {
            return tx.execute(status -> {
                touch(project, uid, false);
                return action.get();
            });
        } catch (DuplicateKeyException conflict) {
            throw new AuthException(AuthErrorStatus.FIREBASE_ENROLLMENT_RESTART_REQUIRED);
        }
    }

    public boolean claim(String project, String uid, Supplier<Boolean> unowned) {
        try {
            return Boolean.TRUE.equals(tx.execute(status -> {
                if (!unowned.get()) return false;
                touch(project, uid, true);
                return true;
            }));
        } catch (DuplicateKeyException conflict) { return false; }
    }

    private void touch(String project, String uid, boolean block) {
        var q = Query.query(Criteria.where("_id").is(key(project, uid)).and("blocked").ne(true));
        var update = new Update().inc("version", 1).set("blocked", block)
                .set("updatedAt", clock.instant()).set("outcome", block ? "CLAIMED" : "ENROLLMENT");
        if (block) update.unset("expiresAt");
        else update.set("expiresAt", clock.instant().plus(Duration.ofDays(1)));
        mongo.findAndModify(q, update, FindAndModifyOptions.options().upsert(true).returnNew(true), Document.class, COLLECTION);
    }

    public void mark(String project, String uid, String outcome, boolean terminal) {
        var update = new Update().set("outcome", outcome).set("updatedAt", clock.instant());
        // Remain blocked after deletion to reject stale in-flight proofs. TTL is not a correctness clock.
        if (terminal) update.set("expiresAt", clock.instant().plus(Duration.ofDays(1)));
        mongo.updateFirst(Query.query(Criteria.where("_id").is(key(project, uid)).and("blocked").is(true)),
                update, COLLECTION);
    }

    private String key(String project, String uid) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((namespace + "\0" + project + "\0" + uid).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
