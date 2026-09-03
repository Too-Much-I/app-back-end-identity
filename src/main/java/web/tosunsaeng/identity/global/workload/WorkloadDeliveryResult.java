package web.tosunsaeng.identity.global.workload;

public record WorkloadDeliveryResult(int statusCode, Integer retryAfterSeconds) {

	public static final int MAX_RETRY_AFTER_SECONDS = 300;

	public WorkloadDeliveryResult {
		if (statusCode < 100 || statusCode > 599) {
			throw new IllegalArgumentException("statusCode must be a valid HTTP status");
		}
		if (retryAfterSeconds != null
				&& (retryAfterSeconds < 1 || retryAfterSeconds > MAX_RETRY_AFTER_SECONDS)) {
			throw new IllegalArgumentException("retryAfterSeconds must be between 1 and 300");
		}
	}
}
