package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.*;
import com.mongodb.client.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.*;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.*;

/** Explicit local-only probe. Uses no application configuration, remote URI or real user data. */
@EnabledIfEnvironmentVariable(named="TMI203_LOCAL_MONGO", matches="true")
class RecoveryReplicaSetTests {
    static final String STREAM = "00000000-0000-4000-8000-000000000001";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    MongoClient client; MongoDatabase db; MongoTemplate mongo; TransactionTemplate tx;
    String database;
    @BeforeEach void setup() {
        client = MongoClients.create("mongodb://127.0.0.1:27039/?replicaSet=tmi203-test&directConnection=true&serverSelectionTimeoutMS=5000");
        database = "tmi203_probe_" + UUID.randomUUID().toString().replace("-", "");
        db = client.getDatabase(database).withWriteConcern(WriteConcern.MAJORITY);
        var factory = new SimpleMongoClientDatabaseFactory(client, database);
        mongo = new MongoTemplate(factory);
        tx = new TransactionTemplate(new MongoTransactionManager(factory,
                TransactionOptions.builder().readConcern(ReadConcern.SNAPSHOT).writeConcern(WriteConcern.MAJORITY).build()));
        db.getCollection("withdrawal_stream").insertOne(new Document("_id", "source").append("streamId", STREAM)
                .append("environment", "test").append("status", "ACTIVE").append("lastSequence", 1L)
                .append("replayFloor", 0L).append("availabilityVersion", 0L));
        for (String name : List.of("users", "withdrawal_journal", "withdrawal_deliveries", "withdrawal_unavailable_ranges")) db.createCollection(name);
        db.getCollection("withdrawal_unavailable_ranges").createIndex(new Document("streamId", 1).append("fromSequence", 1),
                new com.mongodb.client.model.IndexOptions().unique(true));
    }
    @AfterEach void cleanup() {
        if (client != null) {
            if (database != null && database.matches("tmi203_probe_[0-9a-f]{32}")) client.getDatabase(database).drop();
            client.close();
        }
    }
    private MongoRecoveryReader reader() { return new MongoRecoveryReader(client, db, Clock.fixed(NOW, ZoneOffset.UTC), () -> true); }
    @Test void captureReadinessObservesRealSourceClosureAndReplacement() {
        db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), set("captureCutover", Date.from(NOW)));
        var properties = new WithdrawalCaptureProperties(); properties.setRequired(true); properties.setEnvironment("test");
        var readiness = new CaptureReadiness(mongo, properties);
        var purchase = new web.tosunsaeng.identity.domain.auth.purchase.PurchaseProperties();
        purchase.setControlWritesEnabled(true); purchase.setFenceEnabled(true); purchase.setSourceStreamId(STREAM);
        var purchaseReadiness = new web.tosunsaeng.identity.domain.auth.purchase.SourceBoundPurchaseReadiness(readiness,purchase,true);
        assertThat(readiness.ready()).isTrue();
        assertThat(purchaseReadiness.ready()).isTrue();
        db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), set("status", "CLOSED_RESTORE"));
        assertThat(readiness.ready()).isFalse();
        assertThat(purchaseReadiness.ready()).isFalse();
        db.getCollection("withdrawal_stream").insertOne(new Document("_id", "replacement").append("environment", "test")
                .append("streamId", "00000000-0000-4000-8000-000000000002").append("status", "ACTIVE")
                .append("lastSequence", 0L).append("replayFloor", 0L).append("captureCutover", Date.from(NOW)));
        assertThat(readiness.ready()).isTrue();
        assertThat(purchaseReadiness.ready()).isFalse(); // No automatic binding to replacement.
    }
    private void retiredSource(int ranges) {
        var source = RecoveryIndexesTests.stream();
        source.put("availabilityVersion", 0L);
        source.put("status", "CLOSED_RESTORE");
        source.put("closedAt", Date.from(NOW.minus(WithdrawalRecords.RETENTION)));
        db.getCollection("withdrawal_stream").replaceOne(eq("_id", "source"), source);
        var active = RecoveryIndexesTests.stream();
        active.put("_id", "replacement"); active.put("streamId", "00000000-0000-4000-8000-000000000002");
        db.getCollection("withdrawal_stream").insertOne(active);
        db.createCollection(MongoSnapshotStore.RUNS);
        for (long n = 1; n <= ranges; n++) db.getCollection("withdrawal_unavailable_ranges").insertOne(
                new Document("_id", "r-" + n).append("streamId", STREAM).append("fromSequence", n).append("toSequence", n));
    }
    private MongoRetiredSourceCleanup retired(MongoRetiredSourceCleanup.Authority authority) {
        return new MongoRetiredSourceCleanup(mongo, tx, Clock.fixed(NOW, ZoneOffset.UTC), authority);
    }
    @Test void retiredRangesAreBoundedAndRestartResolvesAlreadyRemovedStream() {
        retiredSource(101);
        assertThat(retired((e, s, c) -> {}).batch("test", STREAM)).isEqualTo(MongoRetiredSourceCleanup.Result.RANGES_REMOVED);
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(1);
        assertThat(retired((e, s, c) -> {}).batch("test", STREAM)).isEqualTo(MongoRetiredSourceCleanup.Result.STREAM_REMOVED);
        assertThat(retired((e, s, c) -> {}).batch("test", STREAM)).isEqualTo(MongoRetiredSourceCleanup.Result.ABSENT);
        assertThat(db.getCollection("withdrawal_stream").countDocuments()).isEqualTo(1);
    }
    @Test void retirementRevocationRollsBackRangeDeletionAndSourceVersion() {
        retiredSource(3);
        var first = new AtomicBoolean(true);
        assertThatThrownBy(() -> retired((e, s, c) -> {
            if (!first.getAndSet(false)) throw new SecurityException("synthetic revocation");
        }).batch("test", STREAM)).isInstanceOf(SecurityException.class);
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(3);
        assertThat(db.getCollection("withdrawal_stream").find(eq("streamId", STREAM)).first().getLong("availabilityVersion")).isZero();
    }
    @Test void concurrentSourceChangeAbortsRetirement() {
        retiredSource(3);
        assertThatThrownBy(() -> retired((e, s, c) -> db.getCollection("withdrawal_stream")
                .updateOne(eq("streamId", STREAM), inc("availabilityVersion", 1L))).batch("test", STREAM))
                .isInstanceOf(RuntimeException.class);
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(3);
        assertThat(db.getCollection("withdrawal_stream").find(eq("streamId", STREAM)).first().getLong("availabilityVersion")).isEqualTo(1);
    }
    @Test void retirementWaitsForSnapshotAndJournalReferences() {
        retiredSource(1);
        var cleanup = retired((e, s, c) -> {});
        db.getCollection(MongoSnapshotStore.RUNS).insertOne(new Document("environment", "test").append("consumer", "billing").append("streamId", STREAM));
        assertThat(cleanup.batch("test", STREAM)).isEqualTo(MongoRetiredSourceCleanup.Result.REFERENCED);
        db.getCollection(MongoSnapshotStore.RUNS).deleteMany(new Document());
        db.getCollection("withdrawal_journal").insertOne(new Document("streamId", STREAM));
        assertThat(cleanup.batch("test", STREAM)).isEqualTo(MongoRetiredSourceCleanup.Result.REFERENCED);
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(1);
    }
    @Test void sweepPassesFirstHundredWithoutBridgingRealGaps() {
        db.getCollection("withdrawal_stream").replaceOne(eq("_id", "source"), RecoveryIndexesTests.stream());
        for (long n = 2; n <= 240; n += 2) db.getCollection("withdrawal_unavailable_ranges")
                .insertOne(new Document("_id", "range-" + n).append("streamId", STREAM).append("fromSequence", n).append("toSequence", n));
        var sweep = new MongoSourceSweep(mongo, tx, (environment, streamId) -> { }); // local synthetic authority
        assertThat(sweep.batch("test", STREAM).nextAfterSequence()).isEqualTo(198);
        assertThat(sweep.batch("test", STREAM).nextAfterSequence()).isEqualTo(240);
        assertThat(sweep.batch("test", STREAM).nextAfterSequence()).isZero();
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(120);
        assertThat(db.getCollection("withdrawal_stream").find().first().getLong("replayFloor")).isZero();
    }
    @Test void sweepCarriesOverlapAcrossBatchesAndMissingCarryOnlyResetsProgress() {
        db.getCollection("withdrawal_stream").replaceOne(eq("_id", "source"), RecoveryIndexesTests.stream());
        for (long n = 2; n <= 121; n++) db.getCollection("withdrawal_unavailable_ranges")
                .insertOne(new Document("_id", "range-" + n).append("streamId", STREAM).append("fromSequence", n).append("toSequence", n + 1));
        var sweep = new MongoSourceSweep(mongo, tx, (environment, streamId) -> { });
        sweep.batch("test", STREAM); sweep.batch("test", STREAM);
        var rows = db.getCollection("withdrawal_unavailable_ranges");
        assertThat(rows.countDocuments()).isEqualTo(1);
        assertThat(rows.find().first()).containsEntry("fromSequence", 2L).containsEntry("toSequence", 122L);
        rows.deleteOne(eq("fromSequence", 2L));
        rows.insertOne(new Document("_id", "remaining").append("streamId", STREAM).append("fromSequence", 200L).append("toSequence", 200L));
        assertThat(sweep.batch("test", STREAM).progressReset()).isTrue();
        assertThat(rows.countDocuments()).isEqualTo(1);
        assertThat(db.getCollection("withdrawal_stream").find().first().getLong("replayFloor")).isZero();
    }
    private Document user(String id) { return new Document("_id", id).append("status", "WITHDRAWN").append("withdrawnAt", Date.from(NOW.minusSeconds(60))); }
    @Test void snapshotKeepsHeadAndAllBatchesAtSameTDespiteLateCommit() {
        var users = db.getCollection("users");
        for (int i = 1; i <= 41; i++) users.insertOne(user(String.format("00000000-0000-4000-8000-%012d", i)));
        try (var writer = client.startSession()) {
            writer.startTransaction(TransactionOptions.builder().readConcern(ReadConcern.SNAPSHOT).writeConcern(WriteConcern.MAJORITY).build());
            users.insertOne(writer, user("00000000-0000-4000-8000-000000000099"));
            db.getCollection("withdrawal_stream").updateOne(writer, eq("_id", "source"), set("lastSequence", 2L));
            var first = new AtomicBoolean(true);
            var rows = new ArrayList<SnapshotDigest.Item>();
            var result = reader().snapshot("test", STREAM, item -> {
                if (first.getAndSet(false)) writer.commitTransaction();
                rows.add(item);
            });
            assertThat(result.head()).isEqualTo(1); assertThat(result.count()).isEqualTo(41); assertThat(result.pageCount()).isEqualTo(3);
            assertThat(rows).hasSize(41);
            assertThat(users.countDocuments()).isEqualTo(42);
            assertThat(db.getCollection("withdrawal_stream").find().first().getLong("lastSequence")).isEqualTo(2L);
        }
    }
    @Test void expiryEvidenceAndJournalRollbackTogetherThenCommitTogether() {
        var expired = new WithdrawalRecords.Journal(STREAM, STREAM, 1, STREAM, STREAM,
                NOW.minus(WithdrawalRecords.RETENTION), NOW.minus(WithdrawalRecords.RETENTION));
        mongo.insert(expired);
        var authority = new MongoSourceExpiryEvidence.Authority() {
            public void requireCleanupPermission() { }
            public void requireTimingAndSource(WithdrawalRecords.Journal journal, Instant now, String status) { }
        }; // Synthetic test authority only, never registered in application configuration.
        var evidence = new MongoSourceExpiryEvidence(mongo, "test", authority);
        var failing = new WithdrawalSourceCleanup.Evidence() {
            public void requireCleanupPermission() { evidence.requireCleanupPermission(); }
            public void recordExpiry(WithdrawalRecords.Journal journal, Instant now) {
                evidence.recordExpiry(journal, now); throw new IllegalStateException("synthetic rollback");
            }
        };
        var store = new WithdrawalDeliveryStore(mongo);
        assertThatThrownBy(() -> new WithdrawalSourceCleanup(mongo, store, tx, failing, Clock.fixed(NOW, ZoneOffset.UTC)).batch(1, true))
                .isInstanceOf(IllegalStateException.class);
        assertThat(db.getCollection("withdrawal_journal").countDocuments()).isEqualTo(1);
        assertThat(db.getCollection("withdrawal_stream").find().first().getLong("replayFloor")).isZero();
        var result = new WithdrawalSourceCleanup(mongo, store, tx, evidence, Clock.fixed(NOW, ZoneOffset.UTC)).batch(1, true);
        assertThat(result.removed()).isEqualTo(1);
        assertThat(db.getCollection("withdrawal_journal").countDocuments()).isZero();
        assertThat(db.getCollection("withdrawal_stream").find().first().getLong("replayFloor")).isEqualTo(1);
        var read = reader().feed("test", STREAM, 0, null);
        assertThatThrownBy(() -> FeedPageVerifier.verify(STREAM, 0, read.target(), read.observation()))
                .isInstanceOfSatisfying(FeedPageVerifier.Rejected.class, e -> assertThat(e.failure()).isEqualTo(FeedPageVerifier.Failure.REPLAY_UNAVAILABLE));
    }
    @Test void pointQueryFindsRangeBeyondFirstHundredInRealMongo() {
        db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), set("lastSequence", 102L));
        for (long n = 1; n <= 101; n++) db.getCollection("withdrawal_unavailable_ranges")
                .insertOne(new Document("_id", "range-" + n).append("streamId", STREAM).append("fromSequence", n).append("toSequence", n));
        var read = reader().feed("test", STREAM, 100, 101L);
        assertThat(read.observation().ranges()).containsExactly(new UnavailableRanges.Range(101, 101));
        assertThatThrownBy(() -> FeedPageVerifier.verify(STREAM, 100, read.target(), read.observation()))
                .isInstanceOfSatisfying(FeedPageVerifier.Rejected.class, e -> {
                    assertThat(e.failure()).isEqualTo(FeedPageVerifier.Failure.REPLAY_UNAVAILABLE);
                    assertThat(e.missingSequence()).isEqualTo(101);
                });
    }
}
