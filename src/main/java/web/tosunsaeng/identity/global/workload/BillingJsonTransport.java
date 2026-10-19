package web.tosunsaeng.identity.global.workload;

import java.util.List;
import java.util.Objects;

/** Authentication-neutral event response and failure contract. */
public interface BillingJsonTransport {
    enum FailureKind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }
    record DetailedResult(int status, List<String> retryAfter) {
        public DetailedResult { retryAfter = List.copyOf(retryAfter); }
        @Override public String toString() { return "DetailedResult[status=" + status + "]"; }
    }
    final class TransportException extends RuntimeException {
        private final FailureKind kind;
        public TransportException(FailureKind kind, Throwable cause) {
            super(Objects.requireNonNull(cause)); this.kind = Objects.requireNonNull(kind);
        }
        public FailureKind kind() { return kind; }
    }
    DetailedResult postDetailed(String route, byte[] body);
    default WorkloadDeliveryResult post(String route, byte[] body) {
        var result = postDetailed(route, body);
        return new WorkloadDeliveryResult(result.status(), parseRetryAfter(result.retryAfter()));
    }
    static Integer parseRetryAfter(List<String> values) {
        if (values == null || values.size() != 1) return null;
        String value = values.getFirst();
        if (value == null || !value.matches("[0-9]{1,3}")) return null;
        int seconds = Integer.parseInt(value);
        return seconds >= 1 && seconds <= WorkloadDeliveryResult.MAX_RETRY_AFTER_SECONDS ? seconds : null;
    }
}
