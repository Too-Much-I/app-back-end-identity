package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.support.TransactionTemplate;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;

class WithdrawalOperationsTests {
    final WithdrawalDeliveryStore store=mock(WithdrawalDeliveryStore.class);
    final WithdrawalOperations.Authority authority=mock(WithdrawalOperations.Authority.class);
    final Manager manager=new Manager();
    final WithdrawalOperations ops=new WithdrawalOperations(store,authority,new TransactionTemplate(manager),CLOCK,"test");
    static final String OP="44444444-4444-4444-8444-444444444444";
    @Test void noAuthorityNoDb() {
        doThrow(new SecurityException()).when(authority).requireResumePermission();
        assertThatThrownBy(() -> ops.resume(EVENT,0,OP,"AUTH_FIXED","v1")).isInstanceOf(SecurityException.class);
        verifyNoInteractions(store);
    }
    @Test void resumeCasAndAuditInOneTransaction() {
        var d=delivery(State.BLOCKED_AUTH,5);
        when(store.find(EVENT)).thenReturn(d); when(store.journal(d)).thenReturn(journal());
        when(store.activeStream(journal(),"test")).thenReturn(true);
        when(store.resume(d,OP,"AUTH_FIXED","v1",NOW)).thenReturn(true);
        assertThat(ops.resume(EVENT,0,OP,"AUTH_FIXED","v1")).isTrue();
        verify(authority).auditResume(OP,"AUTH_FIXED","v1",1); assertThat(manager.commits).isEqualTo(1);
    }
    @Test void staleGenerationOrExpiredSourceCannotResume() {
        var d=delivery(State.DEAD_LETTER,20); when(store.find(EVENT)).thenReturn(d); when(store.journal(d)).thenReturn(journal());
        when(store.activeStream(journal(),"test")).thenReturn(true);
        assertThatThrownBy(() -> ops.resume(EVENT,1,OP,"AUTH_FIXED","v1")).isInstanceOf(IllegalStateException.class);
        when(store.journal(d)).thenReturn(new Journal(EVENT,STREAM,1,EVENT,USER,NOW.minus(RETENTION),NOW.minus(RETENTION)));
        assertThatThrownBy(() -> ops.resume(EVENT,0,OP,"AUTH_FIXED","v1")).isInstanceOf(IllegalStateException.class);
        verify(store,never()).resume(any(),any(),any(),any(),any());
    }
    @Test void duplicateOperationMustKeepBinding() {
        var d=new Delivery(EVENT+":BILLING",EVENT,BILLING,State.PENDING,1,20,0,NOW,NOW,null,null,3,null,null,null,null,OP,"AUTH_FIXED","v1");
        when(store.find(EVENT)).thenReturn(d);
        assertThat(ops.resume(EVENT,0,OP,"AUTH_FIXED","v1")).isFalse();
        assertThatThrownBy(() -> ops.resume(EVENT,0,OP,"AUTH_FIXED","v2")).isInstanceOf(IllegalStateException.class);
    }
    @Test void cleanupDryRunNoWritesAndEvidenceFailureRollsBack() {
        var mongo=mock(MongoTemplate.class); var evidence=mock(WithdrawalSourceCleanup.Evidence.class);
        var cleanup=new WithdrawalSourceCleanup(mongo,store,new TransactionTemplate(manager),evidence,CLOCK);
        var expired=new Journal(EVENT,STREAM,1,EVENT,USER,NOW.minus(RETENTION),NOW.minus(RETENTION));
        when(store.expiryCandidates(NOW,10)).thenReturn(List.of(expired));
        assertThat(cleanup.batch(10,false).candidates()).isEqualTo(1); verifyNoInteractions(mongo,evidence);
        when(mongo.findById(EVENT,Journal.class)).thenReturn(expired);
        doThrow(new IllegalStateException("no current generation evidence")).when(evidence).recordExpiry(expired,NOW);
        assertThatThrownBy(() -> cleanup.batch(10,true)).isInstanceOf(IllegalStateException.class);
        verify(mongo,never()).remove(any(Query.class),eq(Journal.class)); assertThat(manager.rollbacks).isEqualTo(1);
    }
    @Test void activeLeaseDelaysCleanup() {
        var mongo=mock(MongoTemplate.class); var evidence=mock(WithdrawalSourceCleanup.Evidence.class);
        var expired=new Journal(EVENT,STREAM,1,EVENT,USER,NOW.minus(RETENTION),NOW.minus(RETENTION));
        when(store.expiryCandidates(NOW,1)).thenReturn(List.of(expired)); when(mongo.findById(EVENT,Journal.class)).thenReturn(expired);
        when(mongo.exists(any(Query.class),eq(Delivery.class))).thenReturn(true);
        var result=new WithdrawalSourceCleanup(mongo,store,new TransactionTemplate(manager),evidence,CLOCK).batch(1,true);
        assertThat(result.leased()).isEqualTo(1); verify(evidence,never()).recordExpiry(any(),any());
    }
}
