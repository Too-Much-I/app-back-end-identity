package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class AbandonedFirebaseCleanupRetryPolicy {

	private final Duration initialDelay;
	private final Duration maxDelay;
	private final DoubleSupplier jitterSource;

	public AbandonedFirebaseCleanupRetryPolicy(
			Duration initialDelay,
			Duration maxDelay,
			DoubleSupplier jitterSource
	) {
		this.initialDelay = requirePositive(initialDelay, "initialDelay");
		this.maxDelay = requirePositive(maxDelay, "maxDelay");
		if (this.initialDelay.compareTo(this.maxDelay) > 0) {
			throw new IllegalArgumentException("initialDelay must not exceed maxDelay");
		}
		this.jitterSource = Objects.requireNonNull(jitterSource);
	}

	public Duration delay(int attemptCount) {
		if (attemptCount < 1) throw new IllegalArgumentException("attemptCount must be positive");
		long delay = initialDelay.toMillis();
		long maximum = maxDelay.toMillis();
		for (int index = 1; index < attemptCount && delay < maximum; index++) {
			delay = Math.min(maximum, delay > maximum / 2 ? maximum : delay * 2);
		}
		double jitter = jitterSource.getAsDouble();
		if (jitter < 0 || jitter >= 1 || Double.isNaN(jitter)) {
			throw new IllegalStateException("jitterSource must return a value in [0, 1)");
		}
		long jittered = Math.round(delay * (0.9d + 0.2d * jitter));
		return Duration.ofMillis(Math.max(1, Math.min(maximum, jittered)));
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative() || required.toMillis() < 1) {
			throw new IllegalArgumentException(fieldName + " must be at least one millisecond");
		}
		return required;
	}
}
