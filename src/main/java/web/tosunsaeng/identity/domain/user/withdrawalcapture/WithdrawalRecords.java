package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Minimal source facts. No serialized provider data or authentication material. */
public final class WithdrawalRecords {
    private WithdrawalRecords() { }
    public static final Duration RETENTION = Duration.ofDays(120);
    public static final String BILLING = "BILLING";
    public enum State { PENDING, IN_FLIGHT, DELIVERED, DEAD_LETTER, BLOCKED_AUTH,
        BLOCKED_RETRY_AFTER, BLOCKED_CLOCK, BLOCKED_HEADER, REPLAY_UNAVAILABLE }

    public static String uuid(String value) {
        if (value == null || !UUID.fromString(value).toString().equals(value))
            throw new IllegalArgumentException("Canonical UUID required.");
        return value;
    }

    @Document("withdrawal_stream")
    public record Stream(@Id String id, String streamId, String environment, String status, long lastSequence,
                         Instant captureCutover, long replayFloor) {
        @Override public String toString() { return "WithdrawalStream[redacted]"; }
    }
    @Document("withdrawal_journal")
    public record Journal(@Id String id, String streamId, long sequence, String eventId,
                          String userId, Instant withdrawnAt, Instant capturedAt) {
        public Journal {
            uuid(eventId); uuid(userId); uuid(streamId);
            if (sequence < 1 || withdrawnAt == null || capturedAt == null)
                throw new IllegalArgumentException("Invalid withdrawal fact.");
        }
        public Instant expiresAt() { return capturedAt.plus(RETENTION); }
        @Override public String toString() { return "WithdrawalJournal[redacted]"; }
    }
    @Document("withdrawal_deliveries")
    public record Delivery(@Id String id, String eventId, String destination, State status,
                          long resumeGeneration, long lifetimeAttemptCount, long generationAttemptCount,
                          Instant notBefore, Instant nextAttemptAt, String leaseOwner, Instant leaseUntil,
                          long leaseVersion, String lastFailureCode, Instant deliveredAt, Instant reviewDueAt,
                          Instant dispatchMarkedAt, String resumeOperationId, String resumeReason, String resumeConfigVersion) {
        @Override public String toString() { return "WithdrawalDelivery[status=" + status + "]"; }
    }
}
