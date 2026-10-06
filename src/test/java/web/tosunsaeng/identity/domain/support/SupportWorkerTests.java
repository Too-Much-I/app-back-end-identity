package web.tosunsaeng.identity.domain.support;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class SupportWorkerTests {
    final Instant now = Instant.parse("2026-10-06T00:00:00Z");
    SupportStore store; SupportNotifier notifier; SupportWorker worker;
    @BeforeEach void setup() {
        store = mock(SupportStore.class); notifier = mock(SupportNotifier.class);
        worker = new SupportWorker(store, notifier, Clock.fixed(now, ZoneOffset.UTC), new SimpleMeterRegistry());
        when(store.content("id")).thenReturn(new SupportStore.Content("synthetic", now.plusSeconds(86400)));
    }
    SupportStore.Delivery claim(int attempts) {
        var d = new SupportStore.Delivery("id", "lease", attempts, now.plusSeconds(86400));
        when(store.claim(now)).thenReturn(d); return d;
    }
    @Test void successfulDeliveryIsMarkedSent() {
        var d = claim(1); when(notifier.send(any(), any())).thenReturn(new SupportNotifier.Outcome(true, false, Duration.ZERO, "OK"));
        worker.runOne(); verify(store).finish(d, "SENT", now, "OK", now);
    }
    @Test void rateLimitWaitIsHonored() {
        var d = claim(1); when(notifier.send(any(), any())).thenReturn(new SupportNotifier.Outcome(false, true, Duration.ofHours(2), "RATE_LIMIT"));
        worker.runOne(); verify(store).finish(d, "RETRY_WAIT", now.plusSeconds(7200), "RATE_LIMIT", now);
    }
    @Test void fifthFailureIsTerminal() {
        var d = claim(5); when(notifier.send(any(), any())).thenReturn(new SupportNotifier.Outcome(false, true, Duration.ZERO, "TRANSPORT"));
        worker.runOne(); verify(store).finish(d, "FAILED", now, "TRANSPORT", now);
    }
    @Test void unknownFifthAttemptDoesNotCauseSixthNetworkCall() {
        var d = claim(6); worker.runOne(); verifyNoInteractions(notifier);
        verify(store).finish(d, "FAILED", now, "EXPIRED_OR_EXHAUSTED", now);
    }
    @Test void deletedContentIsNotSent() {
        var d = claim(1); when(store.content("id")).thenReturn(null);
        worker.runOne(); verifyNoInteractions(notifier); verify(store).finish(d, "FAILED", now, "CONTENT_UNAVAILABLE", now);
    }
    @Test void permanentRejectionDoesNotRetry() {
        var d = claim(1); when(notifier.send(any(), any())).thenReturn(new SupportNotifier.Outcome(false, false, Duration.ZERO, "REMOTE_REJECTED"));
        worker.runOne(); verify(store).finish(d, "FAILED", now, "REMOTE_REJECTED", now);
    }
    @Test void workerDoesNotPropagatePotentiallySensitiveExceptionsToSchedulerLogs() {
        when(store.claim(now)).thenThrow(new RuntimeException("sensitive synthetic payload"));
        assertThatCode(worker::tick).doesNotThrowAnyException();
    }
}
