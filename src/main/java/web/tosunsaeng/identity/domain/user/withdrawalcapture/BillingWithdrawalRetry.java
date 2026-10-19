package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.DoubleSupplier;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.State;

/** Uses infrastructure-synchronized time; no independent clock attestation.
 * No HTTP body/header value is retained in decisions or failure codes. */
public final class BillingWithdrawalRetry {
    public record Decision(State state, String reason, Instant nextAttemptAt) { }
    private final DoubleSupplier random;
    public BillingWithdrawalRetry(DoubleSupplier random) { this.random = random; }
    public Decision classify(int status, List<String> retryAfter, long attempt, Instant receivedAt) {
        if (status == 204) return new Decision(State.DELIVERED, null, null);
        if (status == 401 || status == 403) return blocked(State.BLOCKED_AUTH, "HTTP_AUTH", receivedAt);
        boolean retry = status == 408 || status == 425 || status == 429 || (status >= 500 && status <= 599)
                || status < 200 || status > 599;
        if (!retry) return blocked(State.DEAD_LETTER,
                status >= 200 && status < 300 ? "CONTRACT_ERROR" : "HTTP_PERMANENT", receivedAt);
        Instant next = receivedAt.plusSeconds(backoff(attempt));
        String reason = status < 200 || status > 599 ? "PROTOCOL_ERROR" : "HTTP_RETRYABLE";
        if (status == 429 || status == 503) {
            if (retryAfter.size() > 1) return blocked(State.BLOCKED_HEADER, "AMBIGUOUS_RETRY_AFTER", next);
            if (retryAfter.size() == 1) {
                try {
                    String value = retryAfter.getFirst();
                    if (value.length() > 128) throw new IllegalArgumentException();
                    Instant header = value.matches("[0-9]+") ? receivedAt.plusSeconds(Long.parseLong(value))
                            : ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
                    if (header.isAfter(next)) next = header;
                    if (header.isAfter(receivedAt.plus(Duration.ofDays(1))))
                        return blocked(attempt >= 20 ? State.DEAD_LETTER : State.BLOCKED_RETRY_AFTER, "LONG_RETRY_AFTER", next);
                } catch (RuntimeException invalid) { reason = "INVALID_RETRY_AFTER"; }
            }
        }
        return new Decision(attempt >= 20 ? State.DEAD_LETTER : State.PENDING,
                attempt >= 20 ? "ATTEMPTS_EXHAUSTED" : reason, next);
    }
    public Decision network(long attempt, Instant now) {
        if (attempt >= 20) return blocked(State.DEAD_LETTER, "ATTEMPTS_EXHAUSTED", now);
        return new Decision(State.PENDING, "NETWORK", now.plusSeconds(backoff(attempt)));
    }
    private long backoff(long attempt) {
        long cap = attempt >= 11 ? 3600 : Math.min(3600, 5L << (int)Math.max(0, attempt - 1));
        double fraction = random.getAsDouble();
        if (fraction < 0 || fraction >= 1 || !Double.isFinite(fraction)) throw new IllegalStateException("Invalid jitter.");
        return Math.max(1, (long)(cap * fraction));
    }
    private static Decision blocked(State state, String reason, Instant at) { return new Decision(state, reason, at); }
}
