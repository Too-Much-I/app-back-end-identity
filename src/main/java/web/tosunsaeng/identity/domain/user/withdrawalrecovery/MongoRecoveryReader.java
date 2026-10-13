package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.mongodb.ClientSessionOptions;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Collation;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.bson.BsonTimestamp;
import org.bson.Document;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.Journal;

/** Real driver snapshot sessions (MongoDB 5+). No live-read fallback or automatic database changes.
 * The caller must keep all produced rows private until a fenced READY commit succeeds. */
public final class MongoRecoveryReader {
    private static final Collation SIMPLE = Collation.builder().locale("simple").build();
    private static final Duration BUILD_BUDGET = Duration.ofMinutes(5);
    private final MongoClient client;
    private final MongoDatabase database;
    private final Clock clock;
    private final BooleanSupplier runtimeReady;

    public static final class Unavailable extends RuntimeException {
        private final String failureCode;
        public Unavailable() { this("SNAPSHOT_BUILD_FAILED"); }
        public Unavailable(String failureCode) { super("Recovery snapshot source unavailable."); this.failureCode = failureCode; }
        public String failureCode() { return failureCode; }
    }
    public record SnapshotStart(String streamId, BsonTimestamp atClusterTime, long head, long availabilityVersion,
                                java.util.List<UnavailableRanges.Range> knownRanges) {
        public SnapshotStart { knownRanges = java.util.List.copyOf(knownRanges); }
        @Override public String toString() { return "SnapshotStart[redacted]"; }
    }
    public record SnapshotResult(String streamId, BsonTimestamp atClusterTime, long head,
                                 long availabilityVersion, long count, long pageCount, String digest) {
        @Override public String toString() { return "SnapshotReadResult[redacted]"; }
    }
    public record FeedRead(FeedPageVerifier.Observation observation, long target) {
        @Override public String toString() { return "FeedRead[redacted]"; }
    }

    public MongoRecoveryReader(MongoClient client, MongoDatabase database, Clock clock, BooleanSupplier runtimeReady) {
        this.client = java.util.Objects.requireNonNull(client);
        this.database = database.withReadConcern(ReadConcern.SNAPSHOT).withReadPreference(ReadPreference.primary());
        this.clock = java.util.Objects.requireNonNull(clock);
        this.runtimeReady = java.util.Objects.requireNonNull(runtimeReady);
    }

    public FeedRead feed(String environment, String requestedStream, long after, Long fixedTarget) {
        RecoveryProtocol.uuid4(requestedStream);
        if (environment == null || environment.isBlank() || after < 0 || (fixedTarget != null && fixedTarget < after))
            throw new IllegalArgumentException("Invalid feed bounds.");
        Instant started = clock.instant();
        trusted(started, Duration.ofSeconds(5));
        try (ClientSession session = session()) {
            Document source = source(session, environment);
            String stream = source.getString("streamId");
            long head = nonnegativeLong(source, "lastSequence");
            long floor = nonnegativeLong(source, "replayFloor");
            if (floor > head) throw new Unavailable();
            long target = fixedTarget == null ? head : fixedTarget;
            // Do not query a different stream or impossible window; verifier classifies the result.
            if (!stream.equals(requestedStream) || target > head || after > target) {
                trusted(started, Duration.ofSeconds(5));
                return new FeedRead(new FeedPageVerifier.Observation(stream, head, floor, clock.instant(),
                        java.util.List.of(), java.util.List.of()), target);
            }
            long end = after + Math.min(FeedPageVerifier.PAGE_SIZE, target - after);
            var ranges = PageRangeWitnesses.read(stream, after, target, (id, sequence) -> {
                Document row = database.getCollection("withdrawal_unavailable_ranges")
                        .find(session, and(eq("streamId", id), lte("fromSequence", sequence), gte("toSequence", sequence)))
                        .collation(SIMPLE).sort(orderBy(ascending("fromSequence"), ascending("toSequence"), ascending("_id")))
                        .limit(1).maxTime(1, TimeUnit.SECONDS).first();
                trusted(started, Duration.ofSeconds(5));
                return row == null ? Optional.empty() : Optional.of(range(row));
            });
            var rows = new ArrayList<Journal>();
            if (after < end) {
                try (var cursor = database.getCollection("withdrawal_journal")
                        .find(session, and(eq("streamId", stream), gt("sequence", after), lte("sequence", end)))
                        .collation(SIMPLE).sort(ascending("sequence")).limit(21).maxTime(1, TimeUnit.SECONDS).iterator()) {
                    while (cursor.hasNext()) {
                        Document row = cursor.next();
                        rows.add(new Journal(row.getString("_id"), row.getString("streamId"),
                                nonnegativeLong(row, "sequence"), row.getString("eventId"), row.getString("userId"),
                                date(row, "withdrawnAt"), date(row, "capturedAt")));
                    }
                }
            }
            trusted(started, Duration.ofSeconds(5));
            requireTimestamp(session);
            return new FeedRead(new FeedPageVerifier.Observation(stream, head, floor, clock.instant(), ranges, rows), target);
        } catch (Unavailable known) { throw known; }
        catch (RuntimeException failure) { throw new Unavailable(); } // Never expose driver messages or infer empty success.
    }

    public SnapshotResult snapshot(String environment, String expectedStream, Consumer<SnapshotDigest.Item> privateSink) {
        return snapshot(environment, expectedStream, start -> { }, privateSink);
    }

    public SnapshotResult snapshot(String environment, String expectedStream, Consumer<SnapshotStart> onStart,
                                   Consumer<SnapshotDigest.Item> privateSink) {
        RecoveryProtocol.uuid4(expectedStream);
        if (environment == null || environment.isBlank() || privateSink == null) throw new IllegalArgumentException("Invalid snapshot input.");
        Instant started = clock.instant();
        trusted(started, BUILD_BUDGET);
        try (ClientSession session = session()) {
            Document source = source(session, environment);
            if (!expectedStream.equals(source.getString("streamId"))) throw new Unavailable();
            BsonTimestamp at = requireTimestamp(session);
            long head = nonnegativeLong(source, "lastSequence");
            long floor = nonnegativeLong(source, "replayFloor");
            if (floor > head) throw new Unavailable();
            var known = new ArrayList<UnavailableRanges.Range>();
            if (floor > 0) known.add(new UnavailableRanges.Range(1, floor));
            var gapBytes = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
            try (var ranges = database.getCollection("withdrawal_unavailable_ranges")
                    .find(session, eq("streamId", expectedStream)).collation(SIMPLE)
                    .sort(orderBy(ascending("fromSequence"), ascending("toSequence"), ascending("_id")))
                    .batchSize(100).maxTime(5, TimeUnit.MINUTES).iterator()) {
                while (ranges.hasNext()) {
                    trusted(started, BUILD_BUDGET);
                    var range = range(ranges.next());
                    if (range.toSequence() > head) throw new Unavailable();
                    gapBytes.addObject().put("kind", "SOURCE_RANGE").put("fromSequence", Long.toString(range.fromSequence()))
                            .put("toSequence", Long.toString(range.toSequence()));
                    RecoveryProtocol.encode(gapBytes); // Bound memory; never truncate declared gaps to fit.
                    known.add(range);
                }
            }
            onStart.accept(new SnapshotStart(expectedStream, at, head, nonnegativeLong(source, "availabilityVersion"), known));
            var digest = new SnapshotDigest();
            // Snapshot session pins subsequent find/getMore to the source read's atClusterTime.
            try (var cursor = database.getCollection("users").find(session, eq("status", "WITHDRAWN"))
                    .projection(new Document("_id", 1).append("withdrawnAt", 1)).collation(SIMPLE)
                    .sort(ascending("_id")).batchSize(20).maxTime(5, TimeUnit.MINUTES).iterator()) {
                while (cursor.hasNext()) {
                    trusted(started, BUILD_BUDGET);
                    Document user = cursor.next();
                    var item = new SnapshotDigest.Item(user.getString("_id"), date(user, "withdrawnAt"));
                    digest.append(item);
                    privateSink.accept(item);
                }
            }
            trusted(started, BUILD_BUDGET);
            if (!at.equals(requireTimestamp(session))) throw new Unavailable();
            long count = digest.itemCount();
            long pages = count == 0 ? 1 : 1 + (count - 1) / 20;
            return new SnapshotResult(expectedStream, at, head, nonnegativeLong(source, "availabilityVersion"),
                    count, pages, digest.finish());
        } catch (Unavailable known) { throw known; }
        catch (com.mongodb.MongoException failure) {
            throw new Unavailable(failure.getCode() == 286 || failure.getCode() == 246 ? "SNAPSHOT_HISTORY_LOST"
                    : failure.getCode() == 50 ? "SNAPSHOT_BUILD_TIMEOUT" : "SNAPSHOT_BUILD_FAILED");
        }
        catch (RuntimeException failure) { throw new Unavailable(); }
    }

    private ClientSession session() {
        return client.startSession(ClientSessionOptions.builder().snapshot(true).causallyConsistent(false).build());
    }
    private Document source(ClientSession session, String environment) {
        var found = new ArrayList<Document>();
        database.getCollection("withdrawal_stream").find(session, and(eq("environment", environment), eq("status", "ACTIVE")))
                .collation(SIMPLE).limit(2).maxTime(1, TimeUnit.SECONDS).into(found);
        if (found.size() != 1) throw new Unavailable();
        Document source = found.getFirst();
        RecoveryProtocol.uuid4(source.getString("streamId"));
        nonnegativeLong(source, "availabilityVersion"); // Explicit migration; absent is never version zero.
        requireTimestamp(session);
        return source;
    }
    private void trusted(Instant started, Duration budget) {
        Duration elapsed = Duration.between(started, clock.instant());
        if (!runtimeReady.getAsBoolean() || elapsed.isNegative()) throw new Unavailable();
        if (elapsed.compareTo(budget) >= 0) throw new Unavailable("SNAPSHOT_BUILD_TIMEOUT");
    }
    private static BsonTimestamp requireTimestamp(ClientSession session) {
        BsonTimestamp timestamp = session.getSnapshotTimestamp();
        if (timestamp == null) throw new Unavailable();
        return timestamp;
    }
    static long nonnegativeLong(Document document, String field) {
        if (!(document.get(field) instanceof Long value) || value < 0) throw new Unavailable();
        return value;
    }
    static UnavailableRanges.Range range(Document row) {
        return new UnavailableRanges.Range(nonnegativeLong(row, "fromSequence"), nonnegativeLong(row, "toSequence"));
    }
    private static Instant date(Document document, String field) {
        if (!(document.get(field) instanceof Date value)) throw new Unavailable();
        return value.toInstant();
    }
}
