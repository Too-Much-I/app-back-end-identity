package web.tosunsaeng.identity.domain.support;

import java.time.Duration;

public interface SupportNotifier {
    record Outcome(boolean success, boolean retryable, Duration retryAfter, String category) { }
    Outcome send(String inquiryId, String message);
}
