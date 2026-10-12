package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import com.mongodb.client.result.UpdateResult;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;

class WithdrawalDeliveryStoreTests {
    final MongoTemplate mongo=mock(MongoTemplate.class);
    final WithdrawalDeliveryStore store=new WithdrawalDeliveryStore(mongo);
    @Test void dispatchChecksLeaseAndGenerationAndIncrementsBothBudgets() {
        store.dispatch(delivery(State.IN_FLIGHT,0),NOW);
        var q=ArgumentCaptor.forClass(Query.class); var u=ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(q.capture(),u.capture(),any(FindAndModifyOptions.class),eq(Delivery.class));
        assertThat(q.getValue().getQueryObject().keySet()).contains("leaseOwner","leaseVersion","leaseUntil","resumeGeneration","generationAttemptCount","lifetimeAttemptCount");
        assertThat(u.getValue().getUpdateObject().get("$inc",org.bson.Document.class))
                .containsEntry("generationAttemptCount",1L).containsEntry("lifetimeAttemptCount",1L);
    }
    @Test void staleLeaseResultNotAccepted() {
        when(mongo.updateFirst(any(Query.class),any(Update.class),eq(Delivery.class))).thenReturn(UpdateResult.acknowledged(0,0L,null));
        assertThat(store.finish(delivery(State.IN_FLIGHT,1),new BillingWithdrawalRetry.Decision(State.DELIVERED,null,null),NOW)).isFalse();
    }
    @Test void resumePreservesLifetimeAndFailureHistory() {
        when(mongo.updateFirst(any(Query.class),any(Update.class),eq(Delivery.class))).thenReturn(UpdateResult.acknowledged(1,1L,null));
        store.resume(delivery(State.DEAD_LETTER,20),EVENT,"AUTH_FIXED","v1",NOW);
        var u=ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class),u.capture(),eq(Delivery.class));
        assertThat(u.getValue().getUpdateObject().toString()).doesNotContain("lifetimeAttemptCount","lastFailureCode","reviewDueAt");
    }
    @Test void claimDoesNotConsumeDispatchBudget() {
        store.claim("synthetic-worker",NOW);
        var u=ArgumentCaptor.forClass(Update.class);
        verify(mongo).findAndModify(any(Query.class),u.capture(),any(FindAndModifyOptions.class),eq(Delivery.class));
        assertThat(u.getValue().getUpdateObject().toString()).doesNotContain("AttemptCount","dispatchMarkedAt");
    }
}
