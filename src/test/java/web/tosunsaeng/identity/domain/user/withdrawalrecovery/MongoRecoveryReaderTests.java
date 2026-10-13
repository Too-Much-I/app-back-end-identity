package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mongodb.*;
import com.mongodb.client.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.bson.*;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Adapter/query/session unit tests, NOT replica-set isolation evidence. */
class MongoRecoveryReaderTests {
    static final String STREAM = "00000000-0000-4000-8000-000000000001";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    final MongoClient client = mock(MongoClient.class);
    final MongoDatabase db = mock(MongoDatabase.class);
    final ClientSession session = mock(ClientSession.class);
    final Map<String, MongoCollection<Document>> collections = new HashMap<>();
    Document source;
    MongoRecoveryReader reader;

    @BeforeEach void setup() {
        when(db.withReadConcern(ReadConcern.SNAPSHOT)).thenReturn(db);
        when(db.withReadPreference(ReadPreference.primary())).thenReturn(db);
        when(client.startSession(any(ClientSessionOptions.class))).thenReturn(session);
        when(session.getSnapshotTimestamp()).thenReturn(new BsonTimestamp(100, 1));
        for (String name : List.of("withdrawal_stream", "withdrawal_journal", "withdrawal_unavailable_ranges", "users")) {
            @SuppressWarnings("unchecked") MongoCollection<Document> collection = mock(MongoCollection.class);
            collections.put(name, collection); when(db.getCollection(name)).thenReturn(collection);
        }
        source = new Document("streamId", STREAM).append("lastSequence", 2L).append("replayFloor", 0L).append("availabilityVersion", 0L);
        find("withdrawal_stream", List.of(source));
        find("withdrawal_unavailable_ranges", List.of());
        reader = new MongoRecoveryReader(client, db, Clock.fixed(NOW, ZoneOffset.UTC), () -> true);
    }
    @SuppressWarnings("unchecked") FindIterable<Document> find(String name, List<Document> rows) {
        FindIterable<Document> iterable = mock(FindIterable.class, RETURNS_SELF);
        when(collections.get(name).find(eq(session), any(Bson.class))).thenReturn(iterable);
        when(iterable.into(any())).thenAnswer(invocation -> { Collection<Document> target = invocation.getArgument(0); target.addAll(rows); return target; });
        when(iterable.first()).thenReturn(rows.isEmpty() ? null : rows.getFirst());
        MongoCursor<Document> cursor = mock(MongoCursor.class);
        Iterator<Document> iterator = rows.iterator();
        when(cursor.hasNext()).thenAnswer(i -> iterator.hasNext());
        when(cursor.next()).thenAnswer(i -> iterator.next());
        when(iterable.iterator()).thenReturn(cursor);
        return iterable;
    }
    private Document fact(long sequence) {
        String id = String.format("00000000-0000-4000-8000-%012d", sequence + 10);
        return new Document("_id", id).append("streamId", STREAM).append("sequence", sequence)
                .append("eventId", id).append("userId", id).append("withdrawnAt", Date.from(NOW.minusSeconds(60)))
                .append("capturedAt", Date.from(NOW.minusSeconds(60)));
    }
    @Test void oneSnapshotSessionForHeadJournalAndEveryPoint() {
        find("withdrawal_journal", List.of(fact(1), fact(2)));
        var read = reader.feed("test", STREAM, 0, null);
        assertThat(FeedPageVerifier.verify(STREAM, 0, read.target(), read.observation()).done()).isTrue();
        ArgumentCaptor<ClientSessionOptions> options = ArgumentCaptor.forClass(ClientSessionOptions.class);
        verify(client).startSession(options.capture());
        assertThat(options.getValue().isSnapshot()).isTrue();
        verify(collections.get("withdrawal_unavailable_ranges"), times(2)).find(eq(session), any(Bson.class));
        verify(session).close();
    }
    @Test void queryFiltersPointBeforeLimitNotGlobalFirstHundred() {
        var ranges = find("withdrawal_unavailable_ranges", List.of());
        find("withdrawal_journal", List.of(fact(2)));
        reader.feed("test", STREAM, 1, 2L);
        ArgumentCaptor<Bson> filter = ArgumentCaptor.forClass(Bson.class);
        verify(collections.get("withdrawal_unavailable_ranges")).find(eq(session), filter.capture());
        String json = filter.getValue().toBsonDocument(Document.class, MongoClientSettings.getDefaultCodecRegistry()).toJson();
        assertThat(json).contains("streamId", "fromSequence", "$lte", "toSequence", "$gte");
        verify(ranges).limit(1);
    }
    @Test void snapshotReadsAllTombstonesAtPinnedTAndComputesDigest() {
        Document user = new Document("_id", STREAM).append("withdrawnAt", Date.from(NOW.minusSeconds(90)));
        find("users", List.of(user));
        var items = new ArrayList<SnapshotDigest.Item>();
        var result = reader.snapshot("test", STREAM, items::add);
        assertThat(result.head()).isEqualTo(2); assertThat(result.count()).isEqualTo(1);
        assertThat(result.atClusterTime()).isEqualTo(new BsonTimestamp(100, 1));
        var digest = new SnapshotDigest(); items.forEach(digest::append);
        assertThat(result.digest()).isEqualTo(digest.finish());
        verify(collections.get("users")).find(eq(session), any(Bson.class)); verify(session).close();
    }
    @Test void emptySnapshotHasOnePageAndEmptyDigest() {
        find("users", List.of());
        var result = reader.snapshot("test", STREAM, item -> fail("No rows expected"));
        assertThat(result.count()).isZero(); assertThat(result.pageCount()).isEqualTo(1);
        assertThat(result.digest()).isEqualTo(new SnapshotDigest().finish());
    }
    @Test void missingOrNonInt64MigrationVersionFailsClosed() {
        source.remove("availabilityVersion");
        assertThatThrownBy(() -> reader.feed("test", STREAM, 0, null)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        source.put("availabilityVersion", 0);
        assertThatThrownBy(() -> reader.feed("test", STREAM, 0, null)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
    }
    @Test void driverErrorOrMissingTimestampIsNotEmptySuccess() {
        when(session.getSnapshotTimestamp()).thenReturn(null);
        assertThatThrownBy(() -> reader.feed("test", STREAM, 0, null)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        when(session.getSnapshotTimestamp()).thenReturn(new BsonTimestamp(100, 1));
        when(collections.get("withdrawal_unavailable_ranges").find(eq(session), any(Bson.class))).thenThrow(new RuntimeException("sensitive driver failure"));
        assertThatThrownBy(() -> reader.feed("test", STREAM, 0, null)).isInstanceOf(MongoRecoveryReader.Unavailable.class).hasMessage("Recovery snapshot source unavailable.");
    }
    @Test void missingFactIsGapNotShortSuccessfulPage() {
        find("withdrawal_journal", List.of(fact(1)));
        var read = reader.feed("test", STREAM, 0, null);
        assertThatThrownBy(() -> FeedPageVerifier.verify(STREAM, 0, read.target(), read.observation()))
                .isInstanceOfSatisfying(FeedPageVerifier.Rejected.class, e -> assertThat(e.failure()).isEqualTo(FeedPageVerifier.Failure.COVERAGE_GAP));
    }
    @Test void unknownClockNeverReads() {
        reader = new MongoRecoveryReader(client, db, Clock.fixed(NOW, ZoneOffset.UTC), () -> false);
        assertThatThrownBy(() -> reader.feed("test", STREAM, 0, null)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(client, never()).startSession(any(ClientSessionOptions.class));
    }
}
