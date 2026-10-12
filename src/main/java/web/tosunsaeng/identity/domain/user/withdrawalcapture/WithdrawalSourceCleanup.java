package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.time.Instant;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** I3 supplies the same-transaction generation CAS / coverage evidence. There is no permissive default. */
public final class WithdrawalSourceCleanup {
    public interface Evidence {
        void requireCleanupPermission();
        /** Verify timing bounds; CAS source + current consumer generation, record floor/interval/gap without user IDs.
         * Must execute entirely in the caller's Mongo transaction; never an HTTP call. */
        void recordExpiry(Journal source, Instant now);
    }
    public record Result(int candidates, int removed, int leased) { }
    private final MongoTemplate mongo;
    private final WithdrawalDeliveryStore store;
    private final TransactionTemplate transactions;
    private final Evidence evidence;
    private final Clock clock;
    public WithdrawalSourceCleanup(MongoTemplate mongo, WithdrawalDeliveryStore store, TransactionTemplate transactions,
            Evidence evidence, Clock clock) {
        this.mongo=mongo; this.store=store; this.transactions=transactions; this.evidence=evidence; this.clock=clock;
    }
    public Result batch(int limit, boolean apply) {
        if (apply) evidence.requireCleanupPermission();
        var candidates = store.expiryCandidates(clock.instant(), limit);
        int removed=0, leased=0;
        for (Journal candidate : candidates) {
            if (!apply) continue;
            int outcome = transactions.execute(tx -> {
                Instant now = clock.instant();
                Journal current = mongo.findById(candidate.id(), Journal.class);
                if (current == null || now.isBefore(current.expiresAt())) return 0;
                if (mongo.exists(Query.query(Criteria.where("eventId").is(current.eventId())
                        .and("status").is(State.IN_FLIGHT).and("leaseUntil").gt(now)), Delivery.class)) return 2;
                evidence.recordExpiry(current, now);
                // Deletes contend with claim/dispatch/resume writes; any conflict aborts evidence and deletion.
                mongo.remove(Query.query(Criteria.where("eventId").is(current.eventId())), Delivery.class);
                mongo.remove(Query.query(Criteria.where("_id").is(current.id()).and("capturedAt").is(current.capturedAt())), Journal.class);
                return 1;
            });
            if (outcome == 1) removed++; else if (outcome == 2) leased++;
        }
        return new Result(candidates.size(), removed, leased);
    }
}
