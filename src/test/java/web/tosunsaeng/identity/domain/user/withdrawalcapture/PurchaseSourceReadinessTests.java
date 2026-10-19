package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.*;
import web.tosunsaeng.identity.domain.auth.purchase.*;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

class PurchaseSourceReadinessTests {
    static final String STREAM="00000000-0000-4000-8000-000000000001";
    static final String OTHER="00000000-0000-4000-8000-000000000002";
    static final Instant NOW=Instant.parse("2026-10-13T00:00:00Z");
    final MongoTemplate mongo=mock(MongoTemplate.class);
    final PurchaseProperties purchase=new PurchaseProperties();
    final WithdrawalCaptureProperties capture=new WithdrawalCaptureProperties();
    final WithdrawalRecords.Stream source=new WithdrawalRecords.Stream(STREAM,STREAM,"test","ACTIVE",1,NOW,0);
    PurchaseSourceReadinessTests() {
        capture.setRequired(true); capture.setEnvironment("test");
        purchase.setControlWritesEnabled(true); purchase.setFenceEnabled(true);
        purchase.setScopeEnabled(true); purchase.setSourceStreamId(STREAM);
        when(mongo.find(any(Query.class),eq(WithdrawalRecords.Stream.class))).thenReturn(List.of(source));
    }
    PurchaseReadiness readiness(boolean sessions) {
        return new WithdrawalCaptureConfiguration().withdrawalPurchaseReadiness(new CaptureReadiness(mongo,capture),purchase,sessions);
    }
    @Test void singleValidatedReadBindsExactlyAndNeverUsesFindOne() {
        assertThat(readiness(true).ready()).isTrue();
        var query=org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(),eq(WithdrawalRecords.Stream.class));
        assertThat(query.getValue().getLimit()).isEqualTo(2);
        assertThat(query.getValue().getQueryObject().getString("environment")).isEqualTo("test");
        verifyNoMoreInteractions(mongo);
    }
    @ParameterizedTest @ValueSource(strings={"", "bad", "1-1-1-1-1", "AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA"})
    void malformedBindingNeverQueries(String value) {
        purchase.setSourceStreamId(value);
        assertThat(readiness(true).ready()).isFalse();
        assertThatThrownBy(() -> purchase.validate(Duration.ofMinutes(30),true,true)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mongo);
    }
    @Test void missingBindingAndDisabledFlagsFailClosed() {
        purchase.setSourceStreamId(null); assertThat(readiness(true).ready()).isFalse();
        purchase.setSourceStreamId(STREAM); assertThat(readiness(false).ready()).isFalse();
        purchase.setFenceEnabled(false); assertThat(readiness(true).ready()).isFalse();
        purchase.setFenceEnabled(true); purchase.setControlWritesEnabled(false); assertThat(readiness(true).ready()).isFalse();
        verifyNoInteractions(mongo);
        purchase.setControlWritesEnabled(true); capture.setRequired(false); assertThat(readiness(true).ready()).isFalse();
        verifyNoInteractions(mongo);
    }
    @Test void replacementMissingDuplicateAndDatabaseFailureNeverLearnNewSource() {
        var readiness=readiness(true); assertThat(readiness.ready()).isTrue();
        when(mongo.find(any(Query.class),eq(WithdrawalRecords.Stream.class)))
                .thenReturn(List.of(new WithdrawalRecords.Stream(OTHER,OTHER,"test","ACTIVE",0,NOW,0)))
                .thenReturn(List.of()).thenReturn(List.of(source,source))
                .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic"));
        assertThat(readiness.ready()).isFalse(); assertThat(readiness.ready()).isFalse(); assertThat(readiness.ready()).isFalse();
        assertThatThrownBy(readiness::ready).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(purchase.getSourceStreamId()).isEqualTo(STREAM);
    }
    @Test void realReadinessGuardsIssuanceAndReplayEvenWithScopeOff() {
        var controls=mock(PurchaseControlService.class);
        var manager=mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        var fence=new PurchaseTokenFence(purchase,readiness(true),controls,new TransactionTemplate(manager),
                Clock.fixed(NOW,ZoneOffset.UTC),mock(RefreshSessionRepository.class),mock(SessionSecurityService.class));
        Instant expiry=NOW.plusSeconds(60);
        assertThat(fence.execute(() -> fence.approve(OTHER,UserAccountType.MEMBER,NOW,expiry))).isTrue();
        verify(controls).validateAndTouch(OTHER,UserAccountType.MEMBER,expiry);
        clearInvocations(controls);
        when(mongo.find(any(Query.class),eq(WithdrawalRecords.Stream.class))).thenReturn(List.of());
        assertThatThrownBy(() -> fence.execute(() -> fence.approve(OTHER,UserAccountType.MEMBER,NOW,expiry)))
                .isInstanceOf(web.tosunsaeng.identity.domain.auth.common.exception.AuthException.class);
        purchase.setScopeEnabled(false);
        assertThatThrownBy(() -> fence.execute(() -> { fence.replay(OTHER,UserAccountType.MEMBER,expiry,true); return null; }))
                .isInstanceOf(web.tosunsaeng.identity.domain.auth.common.exception.AuthException.class);
        verifyNoInteractions(controls);
        when(mongo.find(any(Query.class),eq(WithdrawalRecords.Stream.class))).thenReturn(List.of(source));
        fence.execute(() -> { fence.replay(OTHER,UserAccountType.MEMBER,expiry,true); return null; });
        verify(controls).validateAndTouch(OTHER,UserAccountType.MEMBER,expiry);
    }
}
