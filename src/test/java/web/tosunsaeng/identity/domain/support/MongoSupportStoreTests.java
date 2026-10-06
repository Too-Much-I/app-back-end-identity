package web.tosunsaeng.identity.domain.support;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;

/** Verifies concrete filters and writes; does not claim replica-set rollback coverage. */
class MongoSupportStoreTests {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final MongoSupportStore store = new MongoSupportStore(mongo, mock(TransactionTemplate.class));
    final Instant now = Instant.parse("2026-10-06T00:00:00Z");
    @Test void finishRequiresExactLeaseAndSendingState() {
        var d = new SupportStore.Delivery("id", "lease", 1, now.plusSeconds(60));
        store.finish(d, "SENT", now, "OK", now);
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).updateFirst(query.capture(), any(Update.class), eq(MongoSupportStore.DELIVERIES));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "id").containsEntry("status", "SENDING").containsEntry("leaseToken", "lease");
    }
    @Test void insertionWritesOnlyThreeDocumentsAndNoBodyInOutboxOrReceipt() {
        store.insert("id", "key", "digest", null, "ANONYMOUS",
                new SupportRequest(SupportRequest.Category.AUTH, "synthetic long message", "user@example.com", null), now);
        var doc = ArgumentCaptor.forClass(Document.class);
        verify(mongo).insert(doc.capture(), eq(MongoSupportStore.DELIVERIES));
        assertThat(doc.getValue()).doesNotContainKeys("message", "replyEmail", "userId");
        assertThat(doc.getValue().getDate("expiresAt")).isEqualTo(Date.from(now.plus(Duration.ofDays(90))));
        verify(mongo).insert(any(Document.class), eq(MongoSupportStore.INQUIRIES));
        verify(mongo).insert(any(Document.class), eq(MongoSupportStore.REQUESTS));
    }
    @Test void claimIncludesExpiredLeaseAndIncrementsAttemptsAtomically() {
        when(mongo.findAndModify(any(), any(), any(), eq(Document.class), eq(MongoSupportStore.DELIVERIES))).thenReturn(null);
        assertThat(store.claim(now)).isNull();
        var query = ArgumentCaptor.forClass(Query.class); var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(query.capture(), update.capture(), any(), eq(Document.class), eq(MongoSupportStore.DELIVERIES));
        assertThat(query.getValue().getQueryObject().toJson()).contains("leaseUntil", "RETRY_WAIT");
        assertThat(update.getValue().getUpdateObject().get("$inc", Document.class)).containsEntry("attempts", 1);
    }
}
