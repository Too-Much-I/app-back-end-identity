package web.tosunsaeng.identity.domain.support;

import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
import io.micrometer.core.instrument.MeterRegistry;

public final class SupportService {
    public record Actor(String userId, String type) {
        public void requireFor(SupportRequest.Category category) {
            if (category == SupportRequest.Category.REFUND && (userId == null || userId.isBlank())) {
                throw SupportError.SUPPORT_REFUND_AUTH_REQUIRED.exception();
            }
        }
    }
    public record Result(String inquiryId, boolean replay) { }
    private final SupportStore store;
    private final SupportCrypto crypto;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final MeterRegistry metrics;
    public SupportService(SupportStore store, SupportCrypto crypto, ObjectMapper mapper, Clock clock, MeterRegistry metrics) {
        this.store = store; this.crypto = crypto; this.mapper = mapper; this.clock = clock; this.metrics = metrics;
    }
    public void burst(String ip) {
        Instant now = clock.instant();
        guarded(() -> { store.quota(bucket("burst", crypto.hash("ip", ip), now, 60), 30, now.plusSeconds(120)); return null; });
    }
    public Result submit(Actor actor, String ip, String requestId, SupportRequest request) {
        actor.requireFor(request.category());
        if (requestId == null || !requestId.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            throw SupportError.INVALID_SUPPORT_REQUEST_ID.exception();
        }
        String scope = actor.userId() == null ? "ANONYMOUS" : actor.userId();
        String key = crypto.hash("request", scope + ":" + requestId);
        String digest;
        try { digest = crypto.hash("payload", mapper.writeValueAsString(request)); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { throw SupportError.INVALID_REQUEST.exception(); }
        String quotaScope = actor.userId() == null ? crypto.hash("ip", ip) : crypto.hash("user", actor.userId());
        Instant now = clock.instant();
        Result result = guarded(() -> {
            SupportStore.Receipt previous = store.receipt(key);
            if (previous != null && previous.expiresAt().isAfter(now)) {
                if (!previous.digest().equals(digest)) throw SupportError.SUPPORT_INQUIRY_REQUEST_CONFLICT.exception();
                return new Result(previous.inquiryId(), true);
            }
            store.removeExpiredReceipt(key, now);
            store.quota(bucket("hour", quotaScope, now, 3600), 5, now.plus(Duration.ofDays(2)));
            store.quota(bucket("day", quotaScope, now, 86400), 10, now.plus(Duration.ofDays(2)));
            String id = UUID.randomUUID().toString();
            store.insert(id, key, digest, actor.userId(), actor.type(), request, now);
            return new Result(id, false);
        });
        metrics.counter("identity.support.receipts", "outcome", result.replay() ? "replay" : "created").increment();
        return result;
    }
    static String bucket(String kind, String scope, Instant time, long seconds) {
        return kind + ":" + scope + ":" + Math.floorDiv(time.getEpochSecond(), seconds);
    }
    private <T> T guarded(java.util.function.Supplier<T> action) {
        // Whole transaction retry: duplicate-key races and ambiguous commits reuse the exact receipt key.
        for (int attempt = 0; attempt < 3; attempt++) {
            try { return store.transaction(action); }
            catch (DataAccessException | TransactionException e) { /* Never log DB messages or payload. */ }
        }
        metrics.counter("identity.support.receipts", "outcome", "unavailable").increment();
        throw SupportError.SUPPORT_INQUIRY_UNAVAILABLE.exception();
    }
}
