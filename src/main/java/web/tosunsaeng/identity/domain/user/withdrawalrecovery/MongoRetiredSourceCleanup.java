package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.*;
import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords;

/** Removes at most 100 range documents per transaction, never journal or snapshot data. */
public final class MongoRetiredSourceCleanup {
    public interface Authority { void requireRetirement(String environment, String streamId, Instant closedAt); }
    public enum Result { NOT_DUE, REFERENCED, RANGES_REMOVED, STREAM_REMOVED, ABSENT }
    private final MongoTemplate mongo;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Authority authority;
    public MongoRetiredSourceCleanup(MongoTemplate mongo, TransactionTemplate tx, Clock clock, Authority authority) {
        this.mongo = Objects.requireNonNull(mongo); this.clock = Objects.requireNonNull(clock); this.authority = Objects.requireNonNull(authority);
        this.tx = new TransactionTemplate(Objects.requireNonNull(tx.getTransactionManager()), tx); this.tx.setTimeout(30);
    }
    public Result batch(String environment, String streamId) {
        RecoveryProtocol.uuid4(streamId);
        // No automatic retry: unknown commit is resolved by the next fresh read.
        return tx.execute(status -> {
            var source = mongo.findOne(q(Criteria.where("environment").is(environment).and("streamId").is(streamId)), Document.class, "withdrawal_stream");
            if (source == null) return Result.ABSENT;
            RecoveryIndexes.validateStream(source);
            if (!"CLOSED_RESTORE".equals(source.getString("status"))) return Result.NOT_DUE;
            Instant closedAt = source.getDate("closedAt").toInstant();
            if (clock.instant().isBefore(closedAt.plus(WithdrawalRecords.RETENTION))) return Result.NOT_DUE;
            authority.requireRetirement(environment, streamId, closedAt);
            if (!mongo.exists(q(Criteria.where("environment").is(environment).and("status").is("ACTIVE")
                    .and("streamId").ne(streamId)), "withdrawal_stream")) throw new MongoRecoveryReader.Unavailable();
            // Every stream-associated journal must first pass the regular expiry/lease/evidence cleanup.
            if (mongo.exists(q(Criteria.where("streamId").is(streamId)), "withdrawal_journal")
                    || mongo.exists(q(Criteria.where("environment").is(environment).and("consumer").is("billing")
                            .and("streamId").is(streamId)), MongoSnapshotStore.RUNS)) return Result.REFERENCED;
            // Snapshot rows have no streamId. Store maintenance removes rows before the owning run;
            // never scan a nonexistent row field and mistake that for proof of absence.
            long version = MongoRecoveryReader.nonnegativeLong(source, "availabilityVersion");
            var fence = q(Criteria.where("_id").is(source.get("_id")).and("environment").is(environment)
                    .and("streamId").is(streamId).and("status").is("CLOSED_RESTORE").and("closedAt").is(source.get("closedAt"))
                    .and("availabilityVersion").is(version).and("lastSequence").is(source.get("lastSequence")));
            if (mongo.updateFirst(fence, new Update().inc("availabilityVersion", 1L), "withdrawal_stream").getModifiedCount() != 1)
                throw new MongoRecoveryReader.Unavailable();
            var ranges = mongo.find(q(Criteria.where("streamId").is(streamId))
                    .with(org.springframework.data.domain.Sort.by("fromSequence", "toSequence", "_id")).limit(100), Document.class, "withdrawal_unavailable_ranges");
            for (var range : ranges) {
                MongoRecoveryReader.range(range);
                if (!(range.get("_id") instanceof String)) throw new MongoRecoveryReader.Unavailable();
                if (mongo.remove(q(Criteria.where("_id").is(range.get("_id")).and("streamId").is(streamId)
                        .and("fromSequence").is(range.get("fromSequence")).and("toSequence").is(range.get("toSequence"))), "withdrawal_unavailable_ranges").getDeletedCount() != 1)
                    throw new MongoRecoveryReader.Unavailable();
            }
            if (mongo.exists(q(Criteria.where("streamId").is(streamId)), "withdrawal_unavailable_ranges")) return Result.RANGES_REMOVED;
            var remove = q(Criteria.where("_id").is(source.get("_id")).and("environment").is(environment)
                    .and("streamId").is(streamId).and("status").is("CLOSED_RESTORE").and("closedAt").is(source.get("closedAt"))
                    .and("availabilityVersion").is(version + 1));
            authority.requireRetirement(environment, streamId, closedAt);
            if (mongo.remove(remove, "withdrawal_stream").getDeletedCount() != 1) throw new MongoRecoveryReader.Unavailable();
            return Result.STREAM_REMOVED;
        });
    }
    private static Query q(Criteria c) { return Query.query(c).collation(Collation.of("simple")).maxTimeMsec(5000); }
}
