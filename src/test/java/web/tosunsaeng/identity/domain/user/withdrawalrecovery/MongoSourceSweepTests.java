package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import com.mongodb.MongoException;
import com.mongodb.client.result.*;

class MongoSourceSweepTests {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final TransactionStatus status = mock(TransactionStatus.class);
    final MongoSourceSweep.Authority authority = mock(MongoSourceSweep.Authority.class);
    final Document source = RecoveryIndexesTests.stream();
    final String stream = source.getString("streamId");
    MongoSourceSweep sweep;
    @BeforeEach void setup() {
        when(manager.getTransaction(any())).thenReturn(status);
        sweep = new MongoSourceSweep(mongo, new TransactionTemplate(manager), authority);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenReturn(source);
        when(mongo.find(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges"))).thenReturn(List.of());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("withdrawal_stream"))).thenReturn(UpdateResult.acknowledged(1, 1L, null));
        when(mongo.remove(any(Query.class), eq("withdrawal_unavailable_ranges"))).thenReturn(DeleteResult.acknowledged(1));
    }
    @Test void missingCarryResetsOnlyProgressAndPreservesEvidence() {
        source.put("normalizationAfterSequence", 100L); source.put("normalizationCarryFromSequence", 10L);
        var result = sweep.batch("test", stream);
        assertThat(result.progressReset()).isTrue(); assertThat(result.replayFloor()).isZero();
        verify(mongo, never()).remove(any(Query.class), anyString());
        verify(mongo, never()).insert(any(Document.class), anyString()); verify(manager).commit(status);
    }
    @Test void prefixAbsorptionCommitsProgressWithoutDeletingSourceJournal() {
        when(mongo.find(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges")))
                .thenReturn(List.of(new Document("_id", "range").append("fromSequence", 1L).append("toSequence", 5L)));
        var result = sweep.batch("test", stream);
        assertThat(result.replayFloor()).isEqualTo(5); assertThat(result.selected()).isEqualTo(1);
        verify(mongo).remove(any(Query.class), eq("withdrawal_unavailable_ranges"));
        verify(mongo, never()).remove(any(Query.class), eq("withdrawal_journal")); verify(manager).commit(status);
    }
    @Test void casMismatchRollsBackBeforeRangeMutation() {
        when(mongo.updateFirst(any(Query.class), any(Update.class), anyString())).thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThatThrownBy(() -> sweep.batch("test", stream)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(manager).rollback(status); verify(manager, never()).commit(any());
        verify(mongo, never()).remove(any(Query.class), anyString());
    }
    @Test void deniedAuthorityDoesNotStartTransaction() {
        doThrow(new SecurityException()).when(authority).requireSweep("test", stream);
        assertThatThrownBy(() -> sweep.batch("test", stream)).isInstanceOf(SecurityException.class);
        verifyNoInteractions(manager, mongo);
    }
    @Test void onlyKnownAbortedTransactionsCanRetry() {
        var aborted = new MongoException("synthetic"); aborted.addLabel("TransientTransactionError");
        assertThat(MongoSourceSweep.knownAbort(new RuntimeException(aborted))).isTrue();
        aborted.addLabel("UnknownTransactionCommitResult"); assertThat(MongoSourceSweep.knownAbort(aborted)).isFalse();
        assertThat(MongoSourceSweep.knownAbort(new RuntimeException())).isFalse();
    }
    @Test void knownAbortHasThreeAttemptLimit() {
        var aborted = new MongoException("synthetic"); aborted.addLabel("TransientTransactionError");
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenThrow(aborted);
        assertThatThrownBy(() -> sweep.batch("test", stream)).isSameAs(aborted);
        verify(manager, times(3)).rollback(status);
    }
    @Test void unknownCommitIsNeverReplayed() {
        var uncertain = new MongoException("synthetic"); uncertain.addLabel("UnknownTransactionCommitResult");
        doThrow(uncertain).when(manager).commit(status);
        assertThatThrownBy(() -> sweep.batch("test", stream)).isSameAs(uncertain);
        verify(manager, times(1)).getTransaction(any());
    }
}
