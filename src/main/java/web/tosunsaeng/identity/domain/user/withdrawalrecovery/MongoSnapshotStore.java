package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocol.Error.*;

/** Durable snapshot lifecycle. All mutations contend on the run version in a Mongo transaction.
 * Supply a snapshot-read/majority-write transaction manager; no bean, scheduler or DDL is installed. */
public final class MongoSnapshotStore {
    public static final String RUNS = "withdrawal_snapshot_runs";
    public static final String ROWS = "withdrawal_snapshot_rows";
    private static final Duration PRIVACY = Duration.ofDays(7), OPERATION = Duration.ofDays(120), BUILD = Duration.ofMinutes(5);
    private static final Set<String> FAILURES = Set.of("SNAPSHOT_HISTORY_LOST", "SNAPSHOT_BUILD_TIMEOUT", "SNAPSHOT_BUILD_FAILED");
    private final MongoTemplate mongo;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final BooleanSupplier runtimeReady;
    public static final class Rejected extends RuntimeException {
        private final RecoveryProtocol.Error error;
        public Rejected(RecoveryProtocol.Error error) { super(error.message()); this.error = error; }
        public RecoveryProtocol.Error error() { return error; }
    }
    public record View(String snapshotId, String streamId, String status, ObjectNode manifest, String failureCode) {
        public View { if (manifest != null) manifest = manifest.deepCopy(); }
        @Override public ObjectNode manifest() { return manifest == null ? null : manifest.deepCopy(); }
        @Override public String toString() { return "SnapshotView[status=" + status + "]"; }
    }
    public record Lease(String snapshotId, String streamId, RecoveryCursor.Owner owner, String worker, Instant deadline) {
        @Override public String toString() { return "SnapshotLease[redacted]"; }
    }
    public record Page(List<SnapshotDigest.Item> items, boolean done, String lastUser, long pageCount) {
        public Page { items = List.copyOf(items); }
        @Override public String toString() { return "SnapshotPage[redacted]"; }
    }
    public record Pending(RecoveryCursor.Owner owner, String snapshotId) {
        @Override public String toString() { return "PendingSnapshot[redacted]"; }
    }
    public MongoSnapshotStore(MongoTemplate mongo, TransactionTemplate tx, Clock clock, BooleanSupplier runtimeReady) {
        this.mongo = Objects.requireNonNull(mongo);
        this.tx = new TransactionTemplate(Objects.requireNonNull(tx.getTransactionManager()), tx);
        this.tx.setTimeout(tx.getTimeout() > 0 ? Math.min(tx.getTimeout(), 30) : 30);
        this.clock = Objects.requireNonNull(clock); this.runtimeReady = Objects.requireNonNull(runtimeReady);
    }

    public View create(RecoveryCursor.Owner owner, RecoveryProtocol.Create request) {
        RecoveryProtocol.uuid4(request.streamId()); RecoveryProtocol.uuid4(request.operationId());
        Instant created = now(); String id = UUID.randomUUID().toString();
        try {
            return tx.execute(t -> {
                Document existing = mongo.findOne(operation(owner, request.operationId()), Document.class, RUNS);
                if (existing != null) {
                    if (!created.isBefore(date(existing, "operationExpiresAt"))) throw unavailable(); // Wait for bounded physical cleanup.
                    if (!request.streamId().equals(existing.getString("streamId")) || !"ALL_WITHDRAWN".equals(existing.getString("scope"))) throw new Rejected(OPERATION_CONFLICT);
                    return view(owner, existing, false);
                }
                Document source = active(owner.environment(), request.streamId());
                fenceSource(source);
                Document run = new Document("_id", id).append("snapshotId", id).append("environment", owner.environment())
                        .append("consumer", owner.consumer()).append("requestOperationId", request.operationId())
                        .append("streamId", request.streamId()).append("scope", "ALL_WITHDRAWN").append("status", "BUILDING")
                        .append("version", 0L).append("createdAt", Date.from(created)).append("privacyExpiresAt", Date.from(created.plus(PRIVACY)))
                        .append("operationExpiresAt", Date.from(created.plus(OPERATION))).append("buildDeadline", Date.from(created.plus(BUILD)))
                        .append("leaseUntil", Date.from(created)).append("leaseOwner", null).append("phase", "PENDING")
                        .append("rowCount", 0L).append("lastUser", null).append("privacyCleaned", false);
                mongo.insert(run, RUNS);
                return new View(id, request.streamId(), "BUILDING", null, null);
            });
        } catch (RuntimeException failure) {
            // Resolve duplicate/uncertain create against the SAME key; never invent a new operation.
            if (failure instanceof Rejected) throw failure;
            if (!(failure instanceof DuplicateKeyException) && !mongoCommitOrConflict(failure)) throw failure;
            return tx.execute(t -> {
                Document winner = mongo.findOne(operation(owner, request.operationId()), Document.class, RUNS);
                if (winner == null) throw unavailable();
                if (!request.streamId().equals(winner.getString("streamId")) || !"ALL_WITHDRAWN".equals(winner.getString("scope"))) throw new Rejected(OPERATION_CONFLICT);
                return view(owner, winner, false);
            });
        }
    }

    public View status(RecoveryCursor.Owner owner, String snapshotId) {
        RecoveryProtocol.uuid4(snapshotId);
        return tx.execute(t -> view(owner, owned(owner, snapshotId), false));
    }

    public Lease claim(RecoveryCursor.Owner owner, String snapshotId) {
        RecoveryProtocol.uuid4(snapshotId);
        return tx.execute(t -> {
            Document run = owned(owner, snapshotId);
            View state = view(owner, run, false);
            if (!state.status().equals("BUILDING") || run.get("leaseOwner") != null) return null;
            if (!"PENDING".equals(run.getString("phase"))) throw unavailable();
            Instant current = now();
            if (!current.isBefore(date(run, "buildDeadline"))) {
                change(run, new Update().set("status", "FAILED").set("failureCode", "SNAPSHOT_BUILD_TIMEOUT"));
                return null;
            }
            String worker = UUID.randomUUID().toString();
            change(run, new Update().set("leaseOwner", worker).set("leaseUntil", run.get("buildDeadline")).set("phase", "READING"));
            return new Lease(snapshotId, run.getString("streamId"), owner, worker, date(run, "buildDeadline"));
        });
    }

    /** Discovery is only a hint; claim() is the authoritative CAS and must still succeed. */
    public Pending nextPending(String environment) {
        Instant current = now();
        Document run = mongo.findOne(query(Criteria.where("environment").is(environment).and("status").is("BUILDING")
                .and("phase").is("PENDING").and("leaseOwner").is(null).and("leaseUntil").lte(Date.from(current))
                .and("buildDeadline").gt(Date.from(current))).with(Sort.by("leaseUntil", "_id")), Document.class, RUNS);
        if (run == null) return null;
        RecoveryProtocol.uuid4(run.getString("snapshotId"));
        return new Pending(new RecoveryCursor.Owner(run.getString("environment"), run.getString("consumer")), run.getString("snapshotId"));
    }

    public void pin(Lease lease, MongoRecoveryReader.SnapshotStart start) {
        tx.executeWithoutResult(t -> {
            Document run = leased(lease, "READING");
            if (run.containsKey("pinned") || !lease.streamId().equals(start.streamId())) throw unavailable();
            var manifest = manifest(lease.snapshotId(), date(run, "privacyExpiresAt"), start);
            RecoveryProtocol.encode(manifest);
            change(run, new Update().set("pinned", Document.parse(manifest.toString()))
                    .set("sourceVersion", start.availabilityVersion()));
        });
    }

    public void append(Lease lease, List<SnapshotDigest.Item> batch) {
        if (batch.isEmpty() || batch.size() > 20) throw new IllegalArgumentException("Bounded snapshot batch required.");
        var items = List.copyOf(batch);
        tx.executeWithoutResult(t -> {
            Document run = leased(lease, "READING");
            if (!run.containsKey("pinned")) throw unavailable();
            long count = number(run, "rowCount"); String previous = run.getString("lastUser");
            var rows = new ArrayList<Document>();
            for (var item : items) {
                if (previous != null && previous.compareTo(item.userId()) >= 0) throw unavailable();
                rows.add(new Document("_id", UUID.randomUUID().toString()).append("snapshotId", lease.snapshotId())
                        .append("userId", item.userId()).append("withdrawnAt", item.withdrawnAt().toString()));
                previous = item.userId(); count = Math.incrementExact(count);
            }
            change(run, new Update().set("rowCount", count).set("lastUser", previous));
            mongo.insert(rows, ROWS);
        });
    }

    /** Freeze writes before rereading persisted rows; stale append calls can no longer succeed. */
    public void beginVerification(Lease lease) {
        tx.executeWithoutResult(t -> change(leased(lease, "READING"), new Update().set("phase", "VERIFYING")));
    }

    public SnapshotDigest verifyStored(Lease lease) {
        var digest = new SnapshotDigest();
        String after = null;
        while (true) {
            final String cursor = after;
            List<Document> rows = tx.execute(t -> {
                leased(lease, "VERIFYING");
                return rows(lease.snapshotId(), cursor, 20);
            });
            for (var row : rows) digest.append(item(row));
            if (rows.size() < 20) break;
            after = rows.getLast().getString("userId");
        }
        digest.finish();
        return digest;
    }

    public void seal(Lease lease, MongoRecoveryReader.SnapshotResult read, SnapshotDigest stored) {
        tx.executeWithoutResult(t -> {
            Document run = leased(lease, "VERIFYING");
            Document pin = run.get("pinned", Document.class);
            if (pin == null || !lease.streamId().equals(read.streamId()) || number(run, "rowCount") != read.count()
                    || stored.itemCount() != read.count() || !stored.finish().equals(read.digest())
                    || number(run, "sourceVersion") != read.availabilityVersion()
                    || !Long.toString(read.head()).equals(pin.getString("H"))) throw unavailable();
            Document timestamp = pin.get("T", Document.class);
            if (!Integer.toUnsignedString(read.atClusterTime().getTime()).equals(timestamp.getString("seconds"))
                    || !Integer.toUnsignedString(read.atClusterTime().getInc()).equals(timestamp.getString("increment"))) throw unavailable();
            Document source = active(lease.owner().environment(), lease.streamId());
            if (number(source, "availabilityVersion") != read.availabilityVersion() || number(source, "lastSequence") < read.head()) throw unavailable();
            fenceSource(source);
            var json = object(pin);
            long pages = read.count() == 0 ? 1 : 1 + (read.count() - 1) / 20;
            if (pages != read.pageCount()) throw unavailable();
            json.put("itemCount", Long.toString(read.count())).put("pageCount", Long.toString(pages)).put("contentDigest", read.digest());
            RecoveryProtocol.encode(JsonNodeFactory.instance.objectNode().put("snapshotId", lease.snapshotId()).put("status", "READY").set("manifest", json));
            change(run, new Update().set("status", "READY").set("manifest", Document.parse(json.toString()))
                    .unset("pinned").unset("sourceVersion").unset("lastUser").unset("leaseOwner").unset("leaseUntil").unset("phase"));
        });
    }

    public void fail(Lease lease, String code) {
        if (!FAILURES.contains(code)) throw new IllegalArgumentException("Unknown snapshot failure.");
        tx.executeWithoutResult(t -> {
            Document run = mongo.findOne(ownerQuery(lease.owner(), lease.snapshotId()), Document.class, RUNS);
            // A lost READY commit response must never downgrade an already committed snapshot.
            if (run == null || !"BUILDING".equals(run.getString("status")) || !lease.worker().equals(run.getString("leaseOwner"))) return;
            String effective = now().isBefore(date(run, "buildDeadline")) ? code : "SNAPSHOT_BUILD_TIMEOUT";
            change(run, new Update().set("status", "FAILED").set("failureCode", effective).unset("leaseOwner").unset("leaseUntil").unset("phase"));
        });
    }

    public Page page(RecoveryCursor.Owner owner, String snapshotId, String expectedStream, long ordinal,
                     long expectedPages, String afterUser) {
        return tx.execute(t -> {
            Document run = owned(owner, snapshotId); View view = view(owner, run, true);
            if (expectedStream != null && !expectedStream.equals(view.streamId())) throw new Rejected(INVALID_REQUEST);
            var manifest = view.manifest();
            long count = RecoveryProtocol.decimal(manifest.get("itemCount"));
            long pages = RecoveryProtocol.decimal(manifest.get("pageCount"));
            if (ordinal < 0 || ordinal >= pages || (expectedPages >= 0 && expectedPages != pages)
                    || (ordinal == 0 ? afterUser != null : afterUser == null)) throw new Rejected(INVALID_REQUEST);
            if (afterUser != null) web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.uuid(afterUser);
            var rows = rows(snapshotId, afterUser, 21);
            boolean last = ordinal == pages - 1;
            long expected = last ? count - Math.multiplyExact(ordinal, 20) : 20;
            if (rows.size() != (last ? expected : 21)) throw unavailable();
            var items = rows.subList(0, (int) expected).stream().map(MongoSnapshotStore::item).toList();
            String previous = afterUser;
            for (var item : items) {
                if (previous != null && previous.compareTo(item.userId()) >= 0) throw unavailable();
                previous = item.userId();
            }
            if (!now().isBefore(date(run, "privacyExpiresAt"))) throw new Rejected(SNAPSHOT_EXPIRED);
            return new Page(items, last, previous, pages);
        });
    }

    /** At most limit runs and 100 rows/run per call. Never replaces an abandoned build with a new T. */
    public int maintain(int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Bounded maintenance required.");
        Instant current = now();
        var filter = new Criteria().orOperator(
                Criteria.where("privacyExpiresAt").lte(Date.from(current)).and("privacyCleaned").ne(true),
                Criteria.where("operationExpiresAt").lte(Date.from(current)),
                Criteria.where("status").is("BUILDING").and("buildDeadline").lte(Date.from(current)));
        var candidates = mongo.find(query(filter).with(Sort.by("privacyExpiresAt", "_id")).limit(limit), Document.class, RUNS);
        int processed = 0;
        for (var candidate : candidates) {
            tx.executeWithoutResult(t -> {
                Document run = mongo.findById(candidate.get("_id"), Document.class, RUNS);
                if (run == null) return;
                Instant at = now();
                if (!at.isBefore(date(run, "privacyExpiresAt"))) {
                    var selected = rows(run.getString("snapshotId"), null, 100);
                    for (var row : selected) {
                        if (mongo.remove(query(Criteria.where("_id").is(row.get("_id")).and("snapshotId").is(run.getString("snapshotId"))), ROWS)
                                .getDeletedCount() != 1) throw unavailable();
                    }
                    boolean empty = !mongo.exists(query(Criteria.where("snapshotId").is(run.getString("snapshotId"))), ROWS);
                    if (empty && !at.isBefore(date(run, "operationExpiresAt"))) {
                        if (mongo.remove(versionQuery(run), RUNS).getDeletedCount() != 1) throw unavailable();
                    } else {
                        change(run, new Update().set("status", "EXPIRED").set("privacyCleaned", empty)
                                .unset("manifest").unset("pinned").unset("sourceVersion").unset("lastUser").unset("rowCount")
                                .unset("failureCode").unset("phase").unset("leaseOwner").unset("leaseUntil").unset("buildDeadline"));
                    }
                } else if ("BUILDING".equals(run.getString("status")) && !at.isBefore(date(run, "buildDeadline"))) {
                    change(run, new Update().set("status", "FAILED").set("failureCode", "SNAPSHOT_BUILD_TIMEOUT")
                            .unset("leaseOwner").unset("leaseUntil").unset("phase"));
                }
            });
            processed++;
        }
        return processed;
    }

    private Document leased(Lease lease, String phase) {
        Document run = owned(lease.owner(), lease.snapshotId()); Instant current = now();
        if (current.isBefore(date(run, "createdAt"))) throw unavailable();
        if (!"BUILDING".equals(run.getString("status")) || !lease.worker().equals(run.getString("leaseOwner"))
                || !lease.streamId().equals(run.getString("streamId")) || !lease.deadline().equals(date(run, "buildDeadline"))
                || !phase.equals(run.getString("phase")) || !current.isBefore(date(run, "leaseUntil"))
                || !current.isBefore(date(run, "privacyExpiresAt"))) throw unavailable();
        return run;
    }
    private Document owned(RecoveryCursor.Owner owner, String id) {
        RecoveryProtocol.uuid4(id);
        Document run = mongo.findOne(ownerQuery(owner, id), Document.class, RUNS);
        if (run == null || !now().isBefore(date(run, "operationExpiresAt"))) throw new Rejected(SNAPSHOT_NOT_FOUND);
        return run;
    }
    private View view(RecoveryCursor.Owner owner, Document run, boolean page) {
        Instant current = now();
        if (current.isBefore(date(run, "createdAt"))) throw unavailable();
        if (!current.isBefore(date(run, "operationExpiresAt"))) throw new Rejected(SNAPSHOT_NOT_FOUND);
        active(owner.environment(), run.getString("streamId"));
        String state = !current.isBefore(date(run, "privacyExpiresAt")) ? "EXPIRED" : run.getString("status");
        if (page && !state.equals("READY")) throw new Rejected(switch (state) {
            case "BUILDING" -> SNAPSHOT_NOT_READY; case "FAILED" -> SNAPSHOT_FAILED; case "EXPIRED" -> SNAPSHOT_EXPIRED;
            default -> SOURCE_UNAVAILABLE;
        });
        if (!Set.of("BUILDING", "READY", "FAILED", "EXPIRED").contains(state)) throw unavailable();
        ObjectNode manifest = "READY".equals(state) ? object(run.get("manifest", Document.class)) : null;
        String code = "FAILED".equals(state) ? run.getString("failureCode") : null;
        if ("FAILED".equals(state) && !FAILURES.contains(code)) throw unavailable();
        return new View(run.getString("snapshotId"), run.getString("streamId"), state, manifest, code);
    }
    private Document active(String environment, String expected) {
        var sources = mongo.find(query(Criteria.where("environment").is(environment).and("status").is("ACTIVE")).limit(2), Document.class, "withdrawal_stream");
        if (sources.size() != 1) throw unavailable();
        Document source = sources.getFirst();
        if (!expected.equals(source.getString("streamId"))) throw new Rejected(STREAM_REPLACED);
        long head = number(source, "lastSequence");
        if (number(source, "replayFloor") > head) throw unavailable();
        number(source, "availabilityVersion");
        return source;
    }
    private void fenceSource(Document source) {
        long version = number(source, "availabilityVersion");
        if (version == Long.MAX_VALUE) throw unavailable();
        if (mongo.updateFirst(query(Criteria.where("_id").is(source.get("_id")).and("environment").is(source.getString("environment"))
                .and("streamId").is(source.getString("streamId")).and("status").is("ACTIVE").and("availabilityVersion").is(version)),
                new Update().inc("availabilityVersion", 1L), "withdrawal_stream").getModifiedCount() != 1) throw unavailable();
    }
    private void change(Document run, Update update) {
        if (number(run, "version") == Long.MAX_VALUE) throw unavailable();
        if (mongo.updateFirst(versionQuery(run), update.inc("version", 1L), RUNS).getModifiedCount() != 1) throw unavailable();
    }
    private Query versionQuery(Document run) { return query(Criteria.where("_id").is(run.get("_id")).and("version").is(number(run, "version"))); }
    private List<Document> rows(String id, String after, int limit) {
        Criteria filter = Criteria.where("snapshotId").is(id);
        if (after != null) filter.and("userId").gt(after);
        return mongo.find(query(filter).with(Sort.by("userId")).limit(limit), Document.class, ROWS);
    }
    private static Query ownerQuery(RecoveryCursor.Owner owner, String id) { return query(Criteria.where("environment").is(owner.environment()).and("consumer").is(owner.consumer()).and("snapshotId").is(id)); }
    private static Query operation(RecoveryCursor.Owner owner, String operation) { return query(Criteria.where("environment").is(owner.environment()).and("consumer").is(owner.consumer()).and("requestOperationId").is(operation)); }
    private static Query query(Criteria c) { return Query.query(c).collation(Collation.of("simple")).maxTimeMsec(5000); }
    private Instant now() { if (!runtimeReady.getAsBoolean()) throw unavailable(); return clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS); }
    private static long number(Document d, String key) { return MongoRecoveryReader.nonnegativeLong(d, key); }
    private static Instant date(Document d, String key) { if (!(d.get(key) instanceof Date date)) throw unavailable(); return date.toInstant(); }
    private static SnapshotDigest.Item item(Document row) {
        String time = row.getString("withdrawnAt"); Instant at = Instant.parse(time);
        if (!at.toString().equals(time)) throw unavailable();
        return new SnapshotDigest.Item(row.getString("userId"), at);
    }
    private static ObjectNode object(Document document) {
        if (document == null) throw unavailable();
        try { return (ObjectNode) new com.fasterxml.jackson.databind.ObjectMapper().readTree(document.toJson()); }
        catch (java.io.IOException | ClassCastException failure) { throw unavailable(); }
    }
    private static ObjectNode manifest(String id, Instant expires, MongoRecoveryReader.SnapshotStart start) {
        if (start.head() < 0 || start.availabilityVersion() < 0) throw unavailable();
        var node = JsonNodeFactory.instance.objectNode().put("snapshotId", id).put("streamId", start.streamId());
        node.putObject("T").put("seconds", Integer.toUnsignedString(start.atClusterTime().getTime()))
                .put("increment", Integer.toUnsignedString(start.atClusterTime().getInc()));
        node.put("H", Long.toString(start.head())).put("legacyCoverage", "UNKNOWN").put("expiresAt", expires.toString());
        var gaps = node.putArray("knownGaps");
        gaps.addObject().put("kind", "LEGACY_HISTORY").putNull("fromSequence").putNull("toSequence");
        for (var range : start.knownRanges()) {
            if (range.toSequence() > start.head()) throw unavailable();
            gaps.addObject().put("kind", "SOURCE_RANGE").put("fromSequence", Long.toString(range.fromSequence())).put("toSequence", Long.toString(range.toSequence()));
        }
        return node;
    }
    private static boolean mongoCommitOrConflict(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause())
            if (cause instanceof com.mongodb.MongoException mongo && (mongo.hasErrorLabel("UnknownTransactionCommitResult") || mongo.hasErrorLabel("TransientTransactionError"))) return true;
        return false;
    }
    private static Rejected unavailable() { return new Rejected(SOURCE_UNAVAILABLE); }
}
