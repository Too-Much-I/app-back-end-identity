package web.tosunsaeng.identity.domain.auth.purchase;

import java.util.Objects;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.domain.user.domain.entity.User;

/** Explicit, bounded administrative operation. No endpoint, scheduler, or automatic startup execution. */
public final class PurchaseControlMigration {
    public record Result(String nextCursor, int inspected, int ready, int missing, int conflicts, int applied) {
        @Override public String toString() {
            return "Result[cursor=redacted, inspected=" + inspected + ", ready=" + ready
                    + ", missing=" + missing + ", conflicts=" + conflicts + ", applied=" + applied + "]";
        }
    }
    private final MongoTemplate mongo;
    private final TransactionTemplate transactions;
    public PurchaseControlMigration(MongoTemplate mongo, TransactionTemplate transactions) {
        this.mongo = Objects.requireNonNull(mongo); this.transactions = Objects.requireNonNull(transactions);
    }
    public Result batch(String afterUserId, int limit, boolean apply) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Batch must be between 1 and 100.");
        if (afterUserId != null && !canonical(afterUserId)) throw new IllegalArgumentException("Invalid cursor.");
        Query query = afterUserId == null ? new Query() : Query.query(Criteria.where("_id").gt(afterUserId));
        var users = mongo.find(query.with(Sort.by("_id")).limit(limit), User.class);
        int ready = 0, missing = 0, conflicts = 0, applied = 0;
        String cursor = afterUserId;
        for (User selected : users) {
            cursor = selected.getUserId();
            // Any conflict requires another diagnostic pass; cursor advancement is not readiness evidence.
            Kind kind = apply ? transactions.execute(status -> inspect(selected.getUserId(), true))
                    : inspect(selected.getUserId(), false);
            if (kind == Kind.READY) ready++;
            else if (kind == Kind.MISSING) missing++;
            else if (kind == Kind.APPLIED) applied++;
            else conflicts++;
        }
        return new Result(cursor, users.size(), ready, missing, conflicts, applied);
    }
    private enum Kind { READY, MISSING, CONFLICT, APPLIED }
    private Kind inspect(String id, boolean apply) {
        User user = mongo.findById(id, User.class);
        if (user == null || !canonical(id) || user.getUpdatedAt() == null || user.getStatus() == null) return Kind.CONFLICT;
        var control = mongo.findById(id, UserSessionControl.class);
        if (control != null && (control.getActiveLogoutId() != null || control.isMarkingRequired())) return Kind.CONFLICT;
        if (control != null && control.getPurchaseState() == user.getStatus()
                && control.getPurchaseAccountType() == user.getAccountType()) return Kind.READY;
        if (control != null && ((control.getPurchaseState() != null && control.getPurchaseState() != user.getStatus())
                || (control.getPurchaseAccountType() != null && control.getPurchaseAccountType() != user.getAccountType())))
            return Kind.CONFLICT;
        if (!apply) return Kind.MISSING;
        PurchaseControlService.requireTransaction();
        var result = mongo.updateFirst(Query.query(Criteria.where("_id").is(id)
                        .and("status").is(user.getStatus()).and("updatedAt").is(user.getUpdatedAt())),
                new Update().inc("purchaseControlRevision", 1L), User.class);
        if (result.getModifiedCount() != 1) throw new org.springframework.dao.OptimisticLockingFailureException("Migration user changed.");
        if (control == null) {
            control = new UserSessionControl(id);
            control.initializePurchaseState(user.getStatus(), user.getAccountType());
            mongo.insert(control);
        } else {
            control.initializePurchaseState(user.getStatus(), user.getAccountType());
            mongo.save(control);
        }
        return Kind.APPLIED;
    }
    private static boolean canonical(String id) {
        try { return java.util.UUID.fromString(id).toString().equals(id); }
        catch (RuntimeException invalid) { return false; }
    }
}
