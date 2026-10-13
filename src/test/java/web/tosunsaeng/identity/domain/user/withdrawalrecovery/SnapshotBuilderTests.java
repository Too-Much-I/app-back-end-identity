package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.bson.BsonTimestamp;
import org.junit.jupiter.api.Test;

class SnapshotBuilderTests {
    static final String ID = "00000000-0000-4000-8000-000000000001";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    final RecoveryCursor.Owner owner = new RecoveryCursor.Owner("test", "billing");
    final MongoSnapshotStore store = mock(MongoSnapshotStore.class);
    final MongoSnapshotStore.Lease lease = new MongoSnapshotStore.Lease(ID, ID, owner, ID, NOW.plusSeconds(300));

    @Test void unclaimedDoesNotReadOrRewriteExistingRows() {
        SnapshotBuilder.Source source = mock(SnapshotBuilder.Source.class);
        assertThat(new SnapshotBuilder(store, source).build(owner, ID)).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
        verifyNoInteractions(source);
    }
    @Test void discoveryDoesNotRebuildAClaimedOrMissingRun() {
        SnapshotBuilder.Source source = mock(SnapshotBuilder.Source.class);
        var builder = new SnapshotBuilder(store, source);
        assertThat(builder.buildNext("test")).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
        when(store.nextPending("test")).thenReturn(new MongoSnapshotStore.Pending(owner, ID));
        assertThat(builder.buildNext("test")).isEqualTo(SnapshotBuilder.Outcome.NOT_CLAIMED);
        verify(store).claim(owner, ID); verifyNoInteractions(source);
    }
    @Test void batches20ThenRemainderAndVerifiesPersistedRowsBeforeSeal() {
        when(store.claim(owner, ID)).thenReturn(lease);
        var start = new MongoRecoveryReader.SnapshotStart(ID, new BsonTimestamp(1, 2), 3, 4, List.of());
        var digest = new SnapshotDigest();
        when(store.verifyStored(lease)).thenReturn(digest);
        var read = new MongoRecoveryReader.SnapshotResult(ID, new BsonTimestamp(1, 2), 3, 4, 21, 2, "dummy");
        SnapshotBuilder.Source source = (environment, stream, pin, sink) -> {
            pin.accept(start);
            for (int n = 1; n <= 21; n++) sink.accept(new SnapshotDigest.Item(String.format("00000000-0000-4000-8000-%012d", n), NOW));
            return read;
        };
        assertThat(new SnapshotBuilder(store, source).build(owner, ID)).isEqualTo(SnapshotBuilder.Outcome.READY);
        var order = inOrder(store);
        order.verify(store).claim(owner, ID); order.verify(store).pin(lease, start);
        order.verify(store).append(eq(lease), argThat(items -> items.size() == 20));
        order.verify(store).append(eq(lease), argThat(items -> items.size() == 1));
        order.verify(store).beginVerification(lease); order.verify(store).verifyStored(lease); order.verify(store).seal(lease, read, digest);
    }
    @Test void historyLostIsDurableFailureNotNewSnapshotAttempt() {
        when(store.claim(owner, ID)).thenReturn(lease);
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, "FAILED", null, "SNAPSHOT_HISTORY_LOST"));
        SnapshotBuilder.Source source = (e, s, p, rows) -> { throw new MongoRecoveryReader.Unavailable("SNAPSHOT_HISTORY_LOST"); };
        assertThat(new SnapshotBuilder(store, source).build(owner, ID)).isEqualTo(SnapshotBuilder.Outcome.FAILED);
        verify(store).fail(lease, "SNAPSHOT_HISTORY_LOST"); verify(store, never()).seal(any(), any(), any());
    }
    @Test void uncertainSealPreservesDurableReady() {
        when(store.claim(owner, ID)).thenReturn(lease);
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, "READY", null, null));
        SnapshotBuilder.Source source = (e, s, p, rows) -> { throw new IllegalStateException("synthetic uncertainty"); };
        assertThat(new SnapshotBuilder(store, source).build(owner, ID)).isEqualTo(SnapshotBuilder.Outcome.READY);
        verify(store).fail(lease, "SNAPSHOT_BUILD_FAILED");
    }
}
