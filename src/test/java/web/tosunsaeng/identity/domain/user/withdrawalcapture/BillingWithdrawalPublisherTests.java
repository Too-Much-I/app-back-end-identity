package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalTestSupport.*;

class BillingWithdrawalPublisherTests {
    final WithdrawalDeliveryStore store=mock(WithdrawalDeliveryStore.class);
    final BillingWithdrawalPublisher.Transport transport=mock(BillingWithdrawalPublisher.Transport.class);
    final Delivery claimed=delivery(State.IN_FLIGHT,0), dispatched=delivery(State.IN_FLIGHT,1);
    BillingWithdrawalPublisher publisher(boolean enabled) {
        return new BillingWithdrawalPublisher(store,transport,new BillingWithdrawalRetry(() -> .5),
                JsonMapper.builder().findAndAddModules().build(),CLOCK,() -> true,() -> enabled,"test",new SimpleMeterRegistry());
    }
    void seed() {
        when(store.claim(anyString(),eq(NOW))).thenReturn(claimed);
        when(store.journal(claimed)).thenReturn(journal());
        when(store.activeStream(journal(),"test")).thenReturn(true);
        when(store.dispatch(claimed,NOW)).thenReturn(dispatched);
        when(store.finish(any(),any(),eq(NOW))).thenReturn(true);
    }
    @Test void offDoesNotClaim() { assertThat(publisher(false).publishNext()).isFalse(); verifyNoInteractions(store,transport); }
    @Test void dispatchIsDurableBeforeHttpAnd204Only() throws Exception {
        seed(); when(transport.send(any())).thenReturn(new BillingWithdrawalPublisher.Response(204,List.of()));
        publisher(true).publishNext();
        var order=inOrder(store,transport); order.verify(store).dispatch(claimed,NOW); order.verify(transport).send(any());
        verify(store).finish(eq(dispatched),argThat(d -> d.state()==State.DELIVERED),eq(NOW));
        var body=org.mockito.ArgumentCaptor.forClass(byte[].class); verify(transport).send(body.capture());
        var parsed=JsonMapper.builder().findAndAddModules().build().readTree(body.getValue());
        assertThat(parsed.size()).isEqualTo(4); assertThat(parsed.get("eventId").asText()).isEqualTo(EVENT);
        assertThat(parsed.get("withdrawnAt").asText()).isEqualTo(NOW.toString());
        assertThat(parsed.has("sequence")).isFalse();
    }
    @Test void unknownDispatchWriteNeverSends() {
        seed(); when(store.dispatch(claimed,NOW)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("synthetic"));
        assertThatThrownBy(() -> publisher(true).publishNext()).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(transport);
    }
    @Test void lostLeaseDoesNotSend() { seed(); when(store.dispatch(claimed,NOW)).thenReturn(null); publisher(true).publishNext(); verifyNoInteractions(transport); }
    @Test void expiredOrMissingSourceNeverFabricatesEvent() {
        seed(); when(store.journal(claimed)).thenReturn(new Journal(EVENT,STREAM,1,EVENT,USER,NOW.minus(RETENTION),NOW.minus(RETENTION)));
        publisher(true).publishNext(); verifyNoInteractions(transport);
        verify(store).finish(eq(claimed),argThat(d -> d.state()==State.REPLAY_UNAVAILABLE),eq(NOW));
    }
    @Test void wrong2xxIsNotDelivered() {
        seed(); when(transport.send(any())).thenReturn(new BillingWithdrawalPublisher.Response(200,List.of()));
        publisher(true).publishNext(); verify(store).finish(eq(dispatched),argThat(d -> d.state()==State.DEAD_LETTER),eq(NOW));
    }
    @Test void priorMarkerCrashAtTwentiethAttemptCannotDispatchAgain() {
        var exhausted=delivery(State.IN_FLIGHT,20); seed(); when(store.claim(anyString(),eq(NOW))).thenReturn(exhausted);
        when(store.journal(exhausted)).thenReturn(journal());
        publisher(true).publishNext(); verifyNoInteractions(transport);
        verify(store).finish(eq(exhausted),argThat(d -> d.state()==State.DEAD_LETTER),eq(NOW));
    }
}
