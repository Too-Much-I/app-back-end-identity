package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import web.tosunsaeng.identity.global.workload.BillingJsonTransport;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

public final class BillingWithdrawalPublisher {
    public interface Transport { Response send(byte[] body); }
    public record Response(int status, List<String> retryAfter) {
        public Response { retryAfter = List.copyOf(retryAfter); }
        @Override public String toString() { return "WithdrawalResponse[status=" + status + "]"; }
    }
    private record Wire(String eventId, int schemaVersion, String userId, java.time.Instant withdrawnAt) {
        @Override public String toString() { return "WithdrawalWire[redacted]"; }
    }
    private final WithdrawalDeliveryStore store;
    private final Transport transport;
    private final BillingWithdrawalRetry retry;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final BooleanSupplier enabled;
    private final String environment;
    private final String owner = UUID.randomUUID().toString();
    private final MeterRegistry metrics;
    public BillingWithdrawalPublisher(WithdrawalDeliveryStore store, Transport transport, BillingWithdrawalRetry retry,
            ObjectMapper mapper, Clock clock, BooleanSupplier enabled,
            String environment, MeterRegistry metrics) {
        this.store=store; this.transport=transport; this.retry=retry; this.mapper=mapper; this.clock=clock;
        this.enabled=enabled; this.environment=environment; this.metrics=metrics;
    }
    public boolean publishNext() {
        if (!enabled.getAsBoolean()) return false; // OFF stops new claims, not results already in flight.
        Delivery d = store.claim(owner, clock.instant());
        if (d == null) return false;
        Journal journal = store.journal(d);
        if (journal == null || !clock.instant().isBefore(journal.expiresAt()) || !store.activeStream(journal, environment)) {
            finish(d, new BillingWithdrawalRetry.Decision(State.REPLAY_UNAVAILABLE, "SOURCE_UNAVAILABLE", clock.instant()));
            return true;
        }
        if (d.generationAttemptCount() >= 20 || d.lifetimeAttemptCount() == Long.MAX_VALUE) {
            finish(d, new BillingWithdrawalRetry.Decision(State.DEAD_LETTER, "ATTEMPTS_EXHAUSTED", clock.instant()));
            return true;
        }
        byte[] body;
        try {
            body = mapper.writer().without(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValueAsBytes(new Wire(journal.eventId(), 1, journal.userId(), journal.withdrawnAt()));
            if (body.length > 4096) throw new IllegalArgumentException();
        } catch (Exception invalid) {
            finish(d, new BillingWithdrawalRetry.Decision(State.DEAD_LETTER, "INVALID_PAYLOAD", clock.instant()));
            return true;
        }
        // If this write throws (including unknown outcome), no HTTP dispatch occurs. Never decrement the marker.
        Delivery dispatched = store.dispatch(d, clock.instant());
        if (dispatched == null) return true;
        BillingWithdrawalRetry.Decision decision;
        try {
            Response response = transport.send(body);
            decision = retry.classify(response.status(), response.retryAfter(), dispatched.generationAttemptCount(),
                    clock.instant());
        } catch (BillingJsonTransport.TransportException failure) {
            decision = failure.kind() == BillingJsonTransport.FailureKind.CREDENTIAL_UNAVAILABLE
                    ? new BillingWithdrawalRetry.Decision(State.BLOCKED_AUTH, "CREDENTIAL_UNAVAILABLE", clock.instant())
                    : retry.network(dispatched.generationAttemptCount(), clock.instant());
        } catch (RuntimeException unknown) {
            decision = retry.classify(0, List.of(), dispatched.generationAttemptCount(), clock.instant());
        }
        finish(dispatched, decision);
        return true;
    }
    private void finish(Delivery d, BillingWithdrawalRetry.Decision decision) {
        boolean saved = store.finish(d, decision, clock.instant());
        metrics.counter("identity.withdrawal.billing.delivery", "outcome", saved ? decision.state().name() : "LEASE_LOST",
                "reason", decision.reason() == null ? "NONE" : decision.reason()).increment();
    }
}
