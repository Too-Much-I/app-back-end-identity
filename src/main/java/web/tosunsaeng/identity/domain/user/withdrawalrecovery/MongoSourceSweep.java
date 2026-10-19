package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.*;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Bounded range-only normalization. Does not delete source journal/delivery or infer consumer ACK. */
public final class MongoSourceSweep {
    public interface Authority { void requireSweep(String environment, String streamId); }
    public record Result(int selected, long nextAfterSequence, long replayFloor, boolean progressReset) { }
    private final MongoTemplate mongo;
    private final TransactionTemplate tx;
    private final Authority authority;
    public MongoSourceSweep(MongoTemplate mongo, TransactionTemplate tx, Authority authority) {
        this.mongo = Objects.requireNonNull(mongo); this.authority = Objects.requireNonNull(authority);
        this.tx = new TransactionTemplate(Objects.requireNonNull(tx.getTransactionManager()), tx); this.tx.setTimeout(30);
    }
    public Result batch(String environment, String streamId) {
        RecoveryProtocol.uuid4(streamId); authority.requireSweep(environment, streamId);
        // Retry only a known-aborted transaction. Unknown commit must be inspected, never replayed blindly.
        long started = System.nanoTime();
        for (int attempt = 1; ; attempt++) {
            try { return tx.execute(t -> sweep(environment, streamId)); }
            catch (RuntimeException failure) {
                if (attempt >= 3 || System.nanoTime() - started >= 30_000_000_000L || !knownAbort(failure)) throw failure;
            }
        }
    }
    private Result sweep(String environment, String streamId) {
        authority.requireSweep(environment, streamId);
        Document source = mongo.findOne(query(Criteria.where("environment").is(environment).and("streamId").is(streamId)), Document.class, "withdrawal_stream");
        if (source == null) throw unavailable();
        RecoveryIndexes.validateStream(source);
        long floor = number(source, "replayFloor"), head = number(source, "lastSequence"), after = number(source, "normalizationAfterSequence");
        Long carryKey = (Long) source.get("normalizationCarryFromSequence");
        Document carry = carryKey == null ? null : mongo.findOne(query(Criteria.where("streamId").is(streamId).and("fromSequence").is(carryKey)), Document.class, "withdrawal_unavailable_ranges");
        if (carryKey != null && carry == null) {
            // A concurrent expiry normalization may consume the carry. Reset progress only; preserve all evidence.
            cas(source, new Update().set("normalizationAfterSequence", 0L).set("normalizationCarryFromSequence", null));
            return new Result(0, 0, floor, true);
        }
        var fresh = mongo.find(query(Criteria.where("streamId").is(streamId).and("fromSequence").gt(after))
                .with(Sort.by("fromSequence", "toSequence", "_id")).limit(99), Document.class, "withdrawal_unavailable_ranges");
        var plan = RangeSweepPlan.create(floor, head, after, carry == null ? null : MongoRecoveryReader.range(carry),
                fresh.stream().map(MongoRecoveryReader::range).toList());
        var selected = new ArrayList<Document>(); if (carry != null) selected.add(carry); selected.addAll(fresh);
        var ids = new HashSet<Object>();
        for (var row : selected) if (!(row.get("_id") instanceof String) || !ids.add(row.get("_id"))) throw unavailable();
        cas(source, new Update().set("replayFloor", plan.replayFloor()).set("normalizationAfterSequence", plan.nextAfterSequence())
                .set("normalizationCarryFromSequence", plan.nextCarryFromSequence()));
        for (var row : selected) {
            if (mongo.remove(query(Criteria.where("_id").is(row.get("_id")).and("streamId").is(streamId)
                    .and("fromSequence").is(row.get("fromSequence")).and("toSequence").is(row.get("toSequence"))), "withdrawal_unavailable_ranges").getDeletedCount() != 1) throw unavailable();
        }
        for (var range : plan.replacements()) mongo.insert(new Document("_id", UUID.randomUUID().toString()).append("streamId", streamId)
                .append("fromSequence", range.fromSequence()).append("toSequence", range.toSequence()), "withdrawal_unavailable_ranges");
        return new Result(selected.size(), plan.nextAfterSequence(), plan.replayFloor(), false);
    }
    private void cas(Document source, Update update) {
        var condition = Criteria.where("_id").is(source.get("_id")).and("environment").is(source.get("environment"))
                .and("streamId").is(source.get("streamId")).and("status").is(source.get("status"))
                .and("availabilityVersion").is(number(source, "availabilityVersion"))
                .and("lastSequence").is(number(source, "lastSequence")).and("replayFloor").is(number(source, "replayFloor"))
                .and("normalizationAfterSequence").is(number(source, "normalizationAfterSequence"))
                .and("normalizationCarryFromSequence").is(source.get("normalizationCarryFromSequence"));
        if (mongo.updateFirst(query(condition), update.inc("availabilityVersion", 1L), "withdrawal_stream").getModifiedCount() != 1) throw unavailable();
    }
    static boolean knownAbort(Throwable failure) {
        boolean aborted = false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof com.mongodb.MongoException m) {
                if (m.hasErrorLabel("UnknownTransactionCommitResult")) return false;
                if (m.hasErrorLabel("TransientTransactionError")) aborted = true;
            }
        }
        return aborted;
    }
    private static long number(Document source, String field) { return MongoRecoveryReader.nonnegativeLong(source, field); }
    private static Query query(Criteria c) { return Query.query(c).collation(Collation.of("simple")).maxTimeMsec(5000); }
    private static MongoRecoveryReader.Unavailable unavailable() { return new MongoRecoveryReader.Unavailable(); }
}
