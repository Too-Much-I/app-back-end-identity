package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;

class WithdrawalCaptureTests {
    @Test void bsonRoundTripKeepsInt64AndSeparateStreamId() {
        var conversions=org.springframework.data.mongodb.core.convert.MongoCustomConversions.create(a -> {});
        var context=new org.springframework.data.mongodb.core.mapping.MongoMappingContext();
        context.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        var converter=new org.springframework.data.mongodb.core.convert.MappingMongoConverter(
                org.springframework.data.mongodb.core.convert.NoOpDbRefResolver.INSTANCE,context);
        converter.setCustomConversions(conversions); converter.afterPropertiesSet();
        var document=new org.bson.Document(); converter.write(journal(),document);
        assertThat(document.get("sequence")).isInstanceOf(Long.class);
        assertThat(converter.read(Journal.class,document)).isEqualTo(journal());
        var stream=new Stream(STREAM,STREAM,"test","ACTIVE",Long.MAX_VALUE,NOW,0);
        var streamDocument=new org.bson.Document(); converter.write(stream,streamDocument);
        assertThat(streamDocument.getString("_id")).isEqualTo(STREAM);
        assertThat(streamDocument.getString("streamId")).isEqualTo(STREAM);
        assertThat(streamDocument.get("lastSequence")).isEqualTo(Long.MAX_VALUE);
    }
    final MongoTemplate mongo=mock(MongoTemplate.class);
    final Manager manager=new Manager();
    final TransactionTemplate tx=new TransactionTemplate(manager);
    final WithdrawalCaptureService capture=new WithdrawalCaptureService(mongo,"test",CLOCK,() -> true);
    void stream() { when(mongo.findAndModify(any(Query.class),any(Update.class),any(FindAndModifyOptions.class),eq(Stream.class)))
            .thenReturn(new Stream(STREAM,STREAM,"test","ACTIVE",1,NOW,0)); }
    @Test void journalAndPendingDeliveryUseSameLcFactInTransaction() {
        stream(); var event=UserWithdrawnOutbox.create(USER,NOW);
        tx.executeWithoutResult(s -> capture.capture(event,NOW));
        var journal=ArgumentCaptor.forClass(Journal.class); var delivery=ArgumentCaptor.forClass(Delivery.class);
        verify(mongo).insert(journal.capture()); verify(mongo).insert(delivery.capture());
        assertThat(journal.getValue().eventId()).isEqualTo(event.getEventId());
        assertThat(journal.getValue().expiresAt()).isEqualTo(NOW.plus(RETENTION));
        assertThat(delivery.getValue().eventId()).isEqualTo(event.getEventId());
        assertThat(delivery.getValue().status()).isEqualTo(State.PENDING);
        assertThat(manager.commits).isEqualTo(1);
    }
    @Test void insertFailurePropagatesAndRollsBack() {
        stream(); doThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic"))
                .when(mongo).insert(any(Delivery.class));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> capture.capture(UserWithdrawnOutbox.create(USER,NOW),NOW)))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(manager.commits).isZero(); assertThat(manager.rollbacks).isEqualTo(1);
    }
    @Test void missingOrOverflowStreamCannotUpsert() {
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> capture.capture(UserWithdrawnOutbox.create(USER,NOW),NOW)))
                .isInstanceOf(web.tosunsaeng.identity.domain.user.exception.UserException.class);
        verify(mongo,never()).insert(any(Journal.class));
        var query=ArgumentCaptor.forClass(Query.class);
        verify(mongo).findAndModify(query.capture(),any(Update.class),any(FindAndModifyOptions.class),eq(Stream.class));
        assertThat(query.getValue().getQueryObject().toJson()).contains("lastSequence","$lt","ACTIVE");
    }
    @Test void requiresTransactionTimingAndReadiness() {
        assertThatThrownBy(() -> capture.capture(UserWithdrawnOutbox.create(USER,NOW),NOW)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> capture.capture(UserWithdrawnOutbox.create(USER,NOW),NOW.minusSeconds(30))))
                .isInstanceOf(RuntimeException.class);
        var closed=new WithdrawalCaptureService(mongo,"test",CLOCK,() -> false);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> closed.capture(UserWithdrawnOutbox.create(USER,NOW),NOW))).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(mongo);
    }
    @Test void representationsDoNotExposeIdentifiers() {
        assertThat(journal().toString()).doesNotContain(EVENT,USER,STREAM);
        assertThat(delivery(State.PENDING,0).toString()).doesNotContain(EVENT);
    }
}
