package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Instant;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.Journal;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalSourceCleanup;

/** Participates in the cleanup caller's Mongo transaction; never commits independently. */
public final class MongoSourceExpiryEvidence implements WithdrawalSourceCleanup.Evidence {
    public interface Authority {
        void requireCleanupPermission();
        /** Verify trusted clock/capture bounds and explicit permission for the source incarnation/status. */
        void requireTimingAndSource(Journal journal, Instant now, String status);
    }
    private static final String STREAMS = "withdrawal_stream";
    private static final String RANGES = "withdrawal_unavailable_ranges";
    private final MongoTemplate mongo;
    private final String environment;
    private final Authority authority;
    public MongoSourceExpiryEvidence(MongoTemplate mongo, String environment, Authority authority) {
        this.mongo = java.util.Objects.requireNonNull(mongo);
        if (environment == null || environment.isBlank()) throw new IllegalArgumentException("Environment required.");
        this.environment = environment; this.authority = java.util.Objects.requireNonNull(authority);
    }
    @Override public void requireCleanupPermission() { authority.requireCleanupPermission(); }

    @Override public void recordExpiry(Journal journal, Instant now) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.hasResource(mongo.getMongoDatabaseFactory())) throw new MongoRecoveryReader.Unavailable();
        requireCleanupPermission();
        if (now.isBefore(journal.expiresAt())) throw new MongoRecoveryReader.Unavailable();
        Document stream = mongo.findOne(query(Criteria.where("environment").is(environment)
                .and("streamId").is(journal.streamId())), Document.class, STREAMS);
        if (stream == null) throw new MongoRecoveryReader.Unavailable();
        String status = stream.getString("status");
        if (!"ACTIVE".equals(status) && !"CLOSED_RESTORE".equals(status)) throw new MongoRecoveryReader.Unavailable();
        authority.requireTimingAndSource(journal, now, status);
        long version = MongoRecoveryReader.nonnegativeLong(stream, "availabilityVersion");
        long head = MongoRecoveryReader.nonnegativeLong(stream, "lastSequence");
        long floor = MongoRecoveryReader.nonnegativeLong(stream, "replayFloor");
        if (version == Long.MAX_VALUE) throw new MongoRecoveryReader.Unavailable();
        Document exact = mongo.findOne(query(Criteria.where("streamId").is(journal.streamId())
                .and("fromSequence").is(journal.sequence())), Document.class, RANGES);
        var candidates = mongo.find(query(Criteria.where("streamId").is(journal.streamId()))
                .with(Sort.by("fromSequence", "toSequence", "_id")).limit(99), Document.class, RANGES)
                .stream().map(MongoSourceExpiryEvidence::stored).toList();
        var plan = RangeMutationPlan.create(floor, head, journal.sequence(), candidates, exact == null ? null : stored(exact));
        var fence = query(Criteria.where("_id").is(stream.get("_id")).and("environment").is(environment)
                .and("streamId").is(journal.streamId()).and("status").is(status)
                .and("availabilityVersion").is(version).and("lastSequence").is(head).and("replayFloor").is(floor));
        if (mongo.updateFirst(fence, new Update().inc("availabilityVersion", 1L).set("replayFloor", plan.replayFloor()), STREAMS)
                .getModifiedCount() != 1) throw new MongoRecoveryReader.Unavailable();
        for (var original : plan.selected()) {
            if (mongo.remove(query(Criteria.where("_id").is(original.id()).and("streamId").is(journal.streamId())
                    .and("fromSequence").is(original.range().fromSequence()).and("toSequence").is(original.range().toSequence())), RANGES)
                    .getDeletedCount() != 1) throw new MongoRecoveryReader.Unavailable();
        }
        for (var replacement : plan.replacements()) {
            mongo.insert(new Document("_id", UUID.randomUUID().toString()).append("streamId", journal.streamId())
                    .append("fromSequence", replacement.fromSequence()).append("toSequence", replacement.toSequence()), RANGES);
        }
    }
    private static Query query(Criteria criteria) { return Query.query(criteria).collation(Collation.of("simple")); }
    private static RangeMutationPlan.Stored stored(Document document) {
        if (!(document.get("_id") instanceof String id) || id.isBlank()) throw new MongoRecoveryReader.Unavailable();
        return new RangeMutationPlan.Stored(id, MongoRecoveryReader.range(document));
    }
}
