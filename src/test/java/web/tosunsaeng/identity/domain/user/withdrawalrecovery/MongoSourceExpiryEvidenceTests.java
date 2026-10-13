package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords;

class MongoSourceExpiryEvidenceTests {
    static final String STREAM = "00000000-0000-4000-8000-000000000001";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
    final MongoSourceExpiryEvidence.Authority authority = mock(MongoSourceExpiryEvidence.Authority.class);
    final MongoSourceExpiryEvidence evidence = new MongoSourceExpiryEvidence(mongo, "test", authority);
    final WithdrawalRecords.Journal journal = new WithdrawalRecords.Journal(STREAM, STREAM, 2, STREAM, STREAM,
            NOW.minus(WithdrawalRecords.RETENTION), NOW.minus(WithdrawalRecords.RETENTION));
    Document stream;
    @BeforeEach void setup() {
        when(mongo.getMongoDatabaseFactory()).thenReturn(factory);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.bindResource(factory, new Object());
        stream = new Document("_id", "source").append("streamId", STREAM).append("environment", "test")
                .append("status", "ACTIVE").append("lastSequence", 5L).append("replayFloor", 0L).append("availabilityVersion", 4L);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenReturn(stream);
        when(mongo.find(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges"))).thenReturn(List.of());
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq("withdrawal_stream")))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
    }
    @AfterEach void cleanup() {
        TransactionSynchronizationManager.unbindResourceIfPossible(factory);
        TransactionSynchronizationManager.clear();
    }
    @Test void rejectsNonMongoTransactionAndNoPermissionBeforeWrites() {
        TransactionSynchronizationManager.unbindResource(factory);
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(mongo, never()).findOne(any(Query.class), any(), anyString());
        TransactionSynchronizationManager.bindResource(factory, new Object());
        doThrow(new SecurityException()).when(authority).requireCleanupPermission();
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(SecurityException.class);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }
    @Test void recordsMiddleSingletonWithoutAdvancingFloorAndCasVersion() {
        evidence.recordExpiry(journal, NOW);
        ArgumentCaptor<Query> fence = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(fence.capture(), update.capture(), eq("withdrawal_stream"));
        assertThat(fence.getValue().getQueryObject()).containsEntry("availabilityVersion", 4L).containsEntry("lastSequence", 5L);
        assertThat(update.getValue().getUpdateObject().get("$set", Document.class)).containsEntry("replayFloor", 0L);
        ArgumentCaptor<Document> replacement = ArgumentCaptor.forClass(Document.class);
        verify(mongo).insert(replacement.capture(), eq("withdrawal_unavailable_ranges"));
        assertThat(replacement.getValue()).containsEntry("fromSequence", 2L).containsEntry("toSequence", 2L).doesNotContainKeys("userId", "eventId");
    }
    @Test void exactSeedRecordIsSelectedEvenWhenGeneralCandidatesAreEmpty() {
        Document exact = new Document("_id", "existing").append("fromSequence", 2L).append("toSequence", 4L);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges"))).thenReturn(exact);
        when(mongo.remove(any(Query.class), eq("withdrawal_unavailable_ranges"))).thenReturn(DeleteResult.acknowledged(1));
        evidence.recordExpiry(journal, NOW);
        ArgumentCaptor<Query> deleted = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(deleted.capture(), eq("withdrawal_unavailable_ranges"));
        assertThat(deleted.getValue().getQueryObject()).containsEntry("_id", "existing").containsEntry("toSequence", 4L);
        ArgumentCaptor<Document> inserted = ArgumentCaptor.forClass(Document.class);
        verify(mongo).insert(inserted.capture(), eq("withdrawal_unavailable_ranges"));
        assertThat(inserted.getValue()).containsEntry("toSequence", 4L);
    }
    @Test void missingMigrationVersionOrOverflowNeverWrites() {
        stream.remove("availabilityVersion");
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        stream.put("availabilityVersion", Long.MAX_VALUE);
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }
    @Test void casOrSelectedDeleteMismatchPropagatesToCallerRollback() {
        when(mongo.updateFirst(any(Query.class), any(Update.class), anyString())).thenReturn(UpdateResult.acknowledged(0, 0L, null));
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(mongo, never()).insert(any(Document.class), anyString());
        when(mongo.updateFirst(any(Query.class), any(Update.class), anyString())).thenReturn(UpdateResult.acknowledged(1, 1L, null));
        Document exact = new Document("_id", "existing").append("fromSequence", 2L).append("toSequence", 4L);
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_unavailable_ranges"))).thenReturn(exact);
        when(mongo.remove(any(Query.class), anyString())).thenReturn(DeleteResult.acknowledged(0));
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(mongo, never()).insert(any(Document.class), anyString());
    }
    @Test void notYetExpiredAndUnapprovedStatusAreRejected() {
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW.minusMillis(1))).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        stream.put("status", "UNKNOWN");
        assertThatThrownBy(() -> evidence.recordExpiry(journal, NOW)).isInstanceOf(MongoRecoveryReader.Unavailable.class);
        verify(mongo, never()).updateFirst(any(Query.class), any(Update.class), anyString());
    }
}
