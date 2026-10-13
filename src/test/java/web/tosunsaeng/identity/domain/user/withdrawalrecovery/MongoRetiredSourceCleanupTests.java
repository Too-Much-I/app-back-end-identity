package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import com.mongodb.client.result.*;

class MongoRetiredSourceCleanupTests {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final TransactionStatus status = mock(TransactionStatus.class);
    final MongoRetiredSourceCleanup.Authority authority = mock(MongoRetiredSourceCleanup.Authority.class);
    final Document source = RecoveryIndexesTests.stream();
    final Instant now = Instant.parse("2026-10-13T00:00:00Z");
    final String stream = source.getString("streamId");
    MongoRetiredSourceCleanup cleanup;
    @BeforeEach void setup() {
        when(manager.getTransaction(any())).thenReturn(status);
        source.put("status", "CLOSED_RESTORE"); source.put("closedAt", Date.from(now.minus(Duration.ofDays(120))));
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenReturn(source);
        when(mongo.exists(any(Query.class), eq("withdrawal_stream"))).thenReturn(true);
        when(mongo.find(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges"))).thenReturn(List.of());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("withdrawal_stream"))).thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.remove(any(Query.class), anyString())).thenReturn(DeleteResult.acknowledged(1));
        cleanup = new MongoRetiredSourceCleanup(mongo, new TransactionTemplate(manager), Clock.fixed(now, ZoneOffset.UTC), authority);
    }
    @Test void closedExactly120DaysCanBeRemoved() {
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.STREAM_REMOVED);
        verify(manager).commit(status); verify(mongo).remove(any(Query.class), eq("withdrawal_stream"));
        verify(mongo, never()).remove(any(Query.class), eq("withdrawal_journal"));
    }
    @Test void activeOrTooRecentIsNeverRemoved() {
        source.put("closedAt", Date.from(now.minus(Duration.ofDays(120)).plusMillis(1)));
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.NOT_DUE);
        source.put("status", "ACTIVE"); source.put("closedAt", null);
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.NOT_DUE);
        verifyNoInteractions(authority); verify(mongo, never()).remove(any(Query.class), anyString());
    }
    @Test void journalOrSnapshotReferencesBlockRemoval() {
        when(mongo.exists(any(Query.class), eq("withdrawal_journal"))).thenReturn(true);
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.REFERENCED);
        when(mongo.exists(any(Query.class), eq("withdrawal_journal"))).thenReturn(false);
        when(mongo.exists(any(Query.class), eq(MongoSnapshotStore.RUNS))).thenReturn(true);
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.REFERENCED);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }
    @Test void noReplacementOrPermissionMeansNoDeletion() {
        when(mongo.exists(any(Query.class), eq("withdrawal_stream"))).thenReturn(false);
        assertThatThrownBy(() -> cleanup.batch("test", stream)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(manager).rollback(status); verify(mongo, never()).remove(any(Query.class), anyString());
    }
    @Test void deniedApprovalRollsBack() {
        doThrow(new SecurityException()).when(authority).requireRetirement(anyString(), anyString(), any());
        assertThatThrownBy(() -> cleanup.batch("test", stream)).isInstanceOf(SecurityException.class);
        verify(manager).rollback(status); verify(mongo, never()).remove(any(Query.class), anyString());
    }
    @Test void casConflictRollsBackBeforeMutation() {
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("withdrawal_stream"))).thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThatThrownBy(() -> cleanup.batch("test", stream)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(manager).rollback(status); verify(mongo, never()).remove(any(Query.class), anyString());
    }
    @Test void boundedRangeBatchKeepsSourceUntilLastRangeRemoved() {
        when(mongo.find(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges")))
                .thenReturn(List.of(new Document("_id", "range").append("fromSequence", 1L).append("toSequence", 2L)));
        when(mongo.exists(any(Query.class), eq("withdrawal_unavailable_ranges"))).thenReturn(true);
        assertThat(cleanup.batch("test", stream)).isEqualTo(MongoRetiredSourceCleanup.Result.RANGES_REMOVED);
        verify(mongo).find(argThat(q -> q.getLimit() == 100), eq(Document.class), eq("withdrawal_unavailable_ranges"));
        verify(mongo, never()).remove(any(Query.class), eq("withdrawal_stream"));
    }
    @Test void unknownCommitDoesNotReplayTransaction() {
        var failure = new com.mongodb.MongoException("synthetic"); failure.addLabel("UnknownTransactionCommitResult");
        doThrow(failure).when(manager).commit(status);
        assertThatThrownBy(() -> cleanup.batch("test", stream)).isSameAs(failure);
        verify(manager, times(1)).getTransaction(any());
    }
}
