package web.tosunsaeng.identity.domain.support;

import java.time.Instant;
import java.util.function.Supplier;

public interface SupportStore {
    record Receipt(String inquiryId, String digest, Instant expiresAt) { }
    record Delivery(String id, String lease, int attempts, Instant expiresAt) { }
    record Content(String message, Instant expiresAt) {
        @Override public String toString() { return "Content[REDACTED]"; }
    }
    <T> T transaction(Supplier<T> work);
    Receipt receipt(String requestKey);
    void removeExpiredReceipt(String requestKey, Instant now);
    void quota(String key, int limit, Instant expiresAt);
    void insert(String id, String requestKey, String digest, String userId, String actorType,
            SupportRequest request, Instant now);
    Delivery claim(Instant now);
    Content content(String id);
    void finish(Delivery delivery, String status, Instant next, String error, Instant now);
}
