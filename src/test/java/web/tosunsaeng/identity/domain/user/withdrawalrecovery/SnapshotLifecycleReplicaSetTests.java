package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import com.fasterxml.jackson.databind.*;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.IndexOptions;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Only explicitly enabled local synthetic databases. No production settings or external providers. */
@EnabledIfEnvironmentVariable(named="TMI203_LOCAL_MONGO", matches="true")
class SnapshotLifecycleReplicaSetTests {
    static final String STREAM = "00000000-0000-4000-8000-000000000001";
    static final String OTHER = "00000000-0000-4000-8000-000000000002";
    static final String OP = "00000000-0000-4000-8000-000000000003";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    final RecoveryCursor.Owner owner = new RecoveryCursor.Owner("test", "billing");
    final MutableClock clock = new MutableClock();
    MongoClient client; MongoDatabase db; MongoTemplate mongo; MongoSnapshotStore store;
    MongoRecoveryReader reader; RecoverySnapshotService service; RecoveryCursor cursors; String database;

    @BeforeEach void setup() {
        client = MongoClients.create("mongodb://127.0.0.1:27039/?replicaSet=tmi203-test&directConnection=true&serverSelectionTimeoutMS=5000");
        database = "tmi203_snapshot_" + UUID.randomUUID().toString().replace("-", "");
        db = client.getDatabase(database).withWriteConcern(WriteConcern.MAJORITY);
        var factory = new SimpleMongoClientDatabaseFactory(client, database); mongo = new MongoTemplate(factory);
        var tx = new TransactionTemplate(new MongoTransactionManager(factory,
                TransactionOptions.builder().readConcern(ReadConcern.SNAPSHOT).writeConcern(WriteConcern.MAJORITY).build()));
        for (String name : List.of("withdrawal_stream", "withdrawal_unavailable_ranges", "users", MongoSnapshotStore.RUNS, MongoSnapshotStore.ROWS)) db.createCollection(name);
        db.getCollection("withdrawal_stream").insertOne(new Document("_id", "source").append("streamId", STREAM).append("environment", "test")
                .append("status", "ACTIVE").append("lastSequence", 3L).append("replayFloor", 0L).append("availabilityVersion", 0L));
        db.getCollection(MongoSnapshotStore.RUNS).createIndex(new Document("environment", 1).append("consumer", 1).append("requestOperationId", 1), new IndexOptions().unique(true));
        db.getCollection(MongoSnapshotStore.RUNS).createIndex(new Document("snapshotId", 1), new IndexOptions().unique(true));
        db.getCollection(MongoSnapshotStore.ROWS).createIndex(new Document("snapshotId", 1).append("userId", 1), new IndexOptions().unique(true));
        store = new MongoSnapshotStore(mongo, tx, clock, () -> true);
        reader = new MongoRecoveryReader(client, db, clock, () -> true);
        cursors = new RecoveryCursor(Map.of("local", new byte[32]), "local", clock);
        service = new RecoverySnapshotService(store, cursors);
    }
    @AfterEach void cleanup() {
        if (client != null) {
            if (database != null && database.matches("tmi203_snapshot_[0-9a-f]{32}")) client.getDatabase(database).drop();
            client.close();
        }
    }
    private String create() { return store.create(owner, new RecoveryProtocol.Create(STREAM, OP)).snapshotId(); }
    private void users(int count) {
        for (int n = 1; n <= count; n++) db.getCollection("users").insertOne(new Document("_id", String.format("00000000-0000-4000-8000-%012d", n))
                .append("status", "WITHDRAWN").append("withdrawnAt", Date.from(NOW.minusSeconds(n))));
    }
    private SnapshotBuilder builder() { return new SnapshotBuilder(store, reader::snapshot); }
    private JsonNode json(RecoverySnapshotService.Reply reply) throws Exception { return new ObjectMapper().readTree(reply.body()); }
    private RecoverySnapshotService.Reply page(String id, String cursor) { return service.page(owner, new RecoveryProtocol.SnapshotPage(id, cursor), OTHER); }

    @Test void idempotentCreatePrecedesStreamConflictAndOwnerIsIsolated() {
        String id = create();
        assertThat(create()).isEqualTo(id);
        assertThat(store.claim(owner, id)).isNotNull();
        assertThat(store.claim(owner, id)).isNull();
        db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), set("streamId", OTHER));
        assertThatThrownBy(() -> store.create(owner, new RecoveryProtocol.Create(OTHER, OP)))
                .isInstanceOfSatisfying(MongoSnapshotStore.Rejected.class, e -> assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.OPERATION_CONFLICT));
        assertThatThrownBy(this::create).isInstanceOfSatisfying(MongoSnapshotStore.Rejected.class, e -> assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.STREAM_REPLACED));
        assertThatThrownBy(() -> store.status(new RecoveryCursor.Owner("test", "other"), id))
                .isInstanceOfSatisfying(MongoSnapshotStore.Rejected.class, e -> assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.SNAPSHOT_NOT_FOUND));
        assertThat(db.getCollection(MongoSnapshotStore.RUNS).countDocuments()).isEqualTo(1);
    }

    @Test void builds41RowsIntoThreeImmutablePagesWithVerifiedDigestAndReplay() throws Exception {
        users(41); String id = create();
        assertThat(page(id, null).status()).isEqualTo(409);
        assertThat(builder().build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.READY);
        var status = service.status(owner, new RecoveryProtocol.Status(id), OTHER);
        var manifest = json(status).get("manifest");
        assertThat(manifest.get("H").asText()).isEqualTo("3");
        assertThat(manifest.get("itemCount").asText()).isEqualTo("41");
        assertThat(manifest.get("pageCount").asText()).isEqualTo("3");
        assertThat(manifest.get("legacyCoverage").asText()).isEqualTo("UNKNOWN");
        var digest = new SnapshotDigest(); String cursor = null; String firstCursor = null; int pages = 0;
        do {
            var reply = page(id, cursor); assertThat(reply.status()).isEqualTo(200);
            var body = json(reply);
            for (var item : body.get("items")) digest.append(new SnapshotDigest.Item(item.get("userId").asText(), Instant.parse(item.get("withdrawnAt").asText())));
            cursor = body.get("nextCursor").isNull() ? null : body.get("nextCursor").asText();
            if (pages++ == 0) firstCursor = cursor;
        } while (cursor != null);
        assertThat(pages).isEqualTo(3); assertThat(digest.finish()).isEqualTo(manifest.get("contentDigest").asText());
        assertThat(json(page(id, firstCursor)).get("items")).hasSize(20);
        assertThat(create()).isEqualTo(id);
        assertThat(builder().build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
    }

    @Test void emptySnapshotHasOneFinalEmptyPage() throws Exception {
        String id = create(); assertThat(builder().buildNext("test")).isEqualTo(SnapshotBuilder.Outcome.READY);
        assertThat(builder().buildNext("test")).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
        var page = json(page(id, null));
        assertThat(page.get("items")).isEmpty(); assertThat(page.get("done").asBoolean()).isTrue(); assertThat(page.get("nextCursor").isNull()).isTrue();
    }

    @Test void partialSourceFailureIsNeverPublishedAndDoesNotResumeWithNewT() throws Exception {
        users(21); String id = create();
        var broken = new SnapshotBuilder(store, (env, stream, start, sink) -> reader.snapshot(env, stream, start, item -> {
            sink.accept(item);
            if (item.userId().endsWith("000000000021")) throw new MongoRecoveryReader.Unavailable("SNAPSHOT_HISTORY_LOST");
        }));
        assertThat(broken.build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.FAILED);
        assertThat(db.getCollection(MongoSnapshotStore.ROWS).countDocuments()).isEqualTo(20);
        assertThat(store.status(owner, id).failureCode()).isEqualTo("SNAPSHOT_HISTORY_LOST");
        assertThat(json(page(id, null)).get("code").asText()).isEqualTo("SNAPSHOT_FAILED");
        assertThat(builder().build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
    }

    @Test void deadlineFencesOldWorkerAndCannotBeRenewedByRetry() {
        String id = create(); var lease = store.claim(owner, id);
        clock.at.set(NOW.plusSeconds(300));
        store.maintain(10);
        assertThat(store.status(owner, id).failureCode()).isEqualTo("SNAPSHOT_BUILD_TIMEOUT");
        assertThatThrownBy(() -> store.append(lease, List.of(new SnapshotDigest.Item(OTHER, NOW))))
                .isInstanceOf(MongoSnapshotStore.Rejected.class);
        assertThat(store.claim(owner, id)).isNull(); assertThat(create()).isEqualTo(id);
    }
    @Test void abandonedReadingStateCannotStartAgainAtNewTEvenIfOwnerWasLost() {
        String id = create();
        db.getCollection(MongoSnapshotStore.RUNS).updateOne(eq("snapshotId", id), set("phase", "READING"));
        assertThatThrownBy(() -> store.claim(owner, id)).isInstanceOf(MongoSnapshotStore.Rejected.class);
        clock.at.set(NOW.plusSeconds(300)); store.maintain(1);
        assertThat(store.status(owner, id).status()).isEqualTo("FAILED");
    }

    @Test void sevenDayPrivacyThen120DayOperationCleanupAreBoundedAndLogicalExpiryIsImmediate() throws Exception {
        users(121); String id = create(); builder().build(owner, id);
        String cursor = json(page(id, null)).get("nextCursor").asText();
        clock.at.set(NOW.plus(Duration.ofDays(7)));
        assertThat(store.status(owner, id).status()).isEqualTo("EXPIRED");
        assertThat(json(page(id, cursor)).get("code").asText()).isEqualTo("SNAPSHOT_EXPIRED");
        assertThat(create()).isEqualTo(id);
        store.maintain(1);
        assertThat(db.getCollection(MongoSnapshotStore.ROWS).countDocuments()).isEqualTo(21);
        Document run = db.getCollection(MongoSnapshotStore.RUNS).find().first();
        assertThat(run).doesNotContainKeys("manifest", "pinned", "lastUser", "rowCount", "leaseOwner");
        store.maintain(1);
        assertThat(db.getCollection(MongoSnapshotStore.ROWS).countDocuments()).isZero();
        assertThat(db.getCollection(MongoSnapshotStore.RUNS).countDocuments()).isEqualTo(1);
        clock.at.set(NOW.plus(Duration.ofDays(120)));
        assertThat(service.status(owner, new RecoveryProtocol.Status(id), OTHER).status()).isEqualTo(404);
        store.maintain(1); assertThat(db.getCollection(MongoSnapshotStore.RUNS).countDocuments()).isZero();
    }

    @Test void lostReadyCommitResponseResolvesToReadyWithoutDowngrade() {
        users(1); String id = create(); var uncertain = spy(store);
        doAnswer(invocation -> { invocation.callRealMethod(); throw new IllegalStateException("synthetic lost response"); })
                .when(uncertain).seal(any(), any(), any());
        assertThat(new SnapshotBuilder(uncertain, reader::snapshot).build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.READY);
        assertThat(store.status(owner, id).status()).isEqualTo("READY");
    }

    @Test void persistedRowCorruptionBeforeSealFailsDigestCheck() {
        users(21); String id = create();
        var altered = new SnapshotBuilder(store, (env, stream, start, sink) -> {
            var result = reader.snapshot(env, stream, start, sink);
            db.getCollection(MongoSnapshotStore.ROWS).updateOne(eq("snapshotId", id), set("withdrawnAt", NOW.toString()));
            return result;
        });
        assertThat(altered.build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.FAILED);
        assertThat(store.status(owner, id).status()).isEqualTo("FAILED");
    }

    @Test void sourceAvailabilityChangeBeforeSealFailsClosed() {
        users(1); String id = create();
        var changed = new SnapshotBuilder(store, (env, stream, start, sink) -> {
            var result = reader.snapshot(env, stream, start, sink);
            db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), inc("availabilityVersion", 1L));
            return result;
        });
        assertThat(changed.build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.FAILED);
    }

    @Test void missingReadyRowIsNotShortSuccessAndCursorExpiresWithoutRenewal() throws Exception {
        users(41); String id = create(); builder().build(owner, id);
        String cursor = json(page(id, null)).get("nextCursor").asText();
        clock.at.set(NOW.plusSeconds(3500));
        String next = json(page(id, cursor)).get("nextCursor").asText();
        clock.at.set(NOW.plusSeconds(3600));
        assertThat(json(page(id, next)).get("code").asText()).isEqualTo("CURSOR_EXPIRED");
        db.getCollection(MongoSnapshotStore.ROWS).deleteOne(and(eq("snapshotId", id), eq("userId", "00000000-0000-4000-8000-000000000041")));
        assertThat(page(id, cursors.issue(new RecoveryCursor.Binding(owner, RecoveryCursor.Kind.SNAPSHOT, STREAM, id, 2, 3,
                "00000000-0000-4000-8000-000000000040"))).status()).isEqualTo(503);
    }

    @Test void concurrentCreateNeverAllocatesTwoOperations() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1); var ids = ConcurrentHashMap.<String>newKeySet();
            List<Future<?>> jobs = new ArrayList<>();
            for (int n = 0; n < 2; n++) jobs.add(executor.submit(() -> {
                start.await();
                try { ids.add(create()); }
                catch (MongoSnapshotStore.Rejected e) { assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.SOURCE_UNAVAILABLE); }
                return null;
            }));
            start.countDown(); for (var job : jobs) job.get(10, TimeUnit.SECONDS);
            ids.add(create()); assertThat(ids).hasSize(1);
            assertThat(db.getCollection(MongoSnapshotStore.RUNS).countDocuments()).isEqualTo(1);
        }
    }

    @Test void oversizedKnownGapsFailBuildWithoutDroppingEvidence() {
        db.getCollection("withdrawal_stream").updateOne(eq("_id", "source"), set("lastSequence", 500L));
        for (long n = 1; n <= 300; n++) db.getCollection("withdrawal_unavailable_ranges").insertOne(
                new Document("_id", "r" + n).append("streamId", STREAM).append("fromSequence", n).append("toSequence", n));
        String id = create(); assertThat(builder().build(owner, id)).isEqualTo(SnapshotBuilder.Outcome.FAILED);
        assertThat(db.getCollection("withdrawal_unavailable_ranges").countDocuments()).isEqualTo(300);
    }

    private static final class MutableClock extends Clock {
        final AtomicReference<Instant> at = new AtomicReference<>(NOW);
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return at.get(); }
    }
}
