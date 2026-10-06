package web.tosunsaeng.identity.domain.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;

public final class SupportWorker {
    private final SupportStore store;
    private final SupportNotifier notifier;
    private final Clock clock;
    private final MeterRegistry metrics;
    public SupportWorker(SupportStore store, SupportNotifier notifier, Clock clock, MeterRegistry metrics) {
        this.store = store; this.notifier = notifier; this.clock = clock; this.metrics = metrics;
    }
    @Scheduled(fixedDelayString="${app.support.worker-delay-ms:5000}")
    public void tick() {
        try { runOne(); }
        catch (RuntimeException e) {
            // Scheduler must not log exception messages which may contain documents or webhook credentials.
            metrics.counter("identity.support.delivery", "outcome", "store_failure").increment();
        }
    }
    public void runOne() {
        Instant now = clock.instant();
        var d = store.claim(now);
        if (d == null) return;
        if (!d.expiresAt().isAfter(now) || d.attempts() > 5) {
            finish(d, "FAILED", now, "EXPIRED_OR_EXHAUSTED"); return;
        }
        var content = store.content(d.id());
        if (content == null || !content.expiresAt().isAfter(now)) {
            finish(d, "FAILED", now, "CONTENT_UNAVAILABLE"); return;
        }
        var outcome = notifier.send(d.id(), content.message());
        now = clock.instant();
        if (outcome.success()) { finish(d, "SENT", now, "OK"); return; }
        if (!outcome.retryable() || d.attempts() >= 5 || outcome.retryAfter().compareTo(Duration.ofDays(1)) > 0) {
            finish(d, "FAILED", now, outcome.category()); return;
        }
        long[] seconds = {30, 120, 600, 1800};
        long delay = seconds[d.attempts() - 1] + ThreadLocalRandom.current().nextLong(16);
        delay = Math.max(delay, outcome.retryAfter().getSeconds());
        Instant next = now.plusSeconds(delay);
        finish(d, next.isBefore(d.expiresAt()) ? "RETRY_WAIT" : "FAILED", next, outcome.category());
    }
    private void finish(SupportStore.Delivery d, String status, Instant next, String error) {
        store.finish(d, status, next, error, clock.instant());
        metrics.counter("identity.support.delivery", "outcome", status.toLowerCase(java.util.Locale.ROOT)).increment();
    }
}
