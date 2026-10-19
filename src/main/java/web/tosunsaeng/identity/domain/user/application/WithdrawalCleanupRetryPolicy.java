package web.tosunsaeng.identity.domain.user.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class WithdrawalCleanupRetryPolicy {

	private static final double MIN_JITTER_FACTOR = 0.9d;
	private static final double JITTER_RANGE = 0.2d;

	private final Duration initialDelay;
	private final Duration maxDelay;
	private final DoubleSupplier jitterSource;

	public WithdrawalCleanupRetryPolicy(
			Duration initialDelay,
			Duration maxDelay,
			DoubleSupplier jitterSource
	) {
		this.initialDelay = requirePositive(initialDelay, "initialDelay");
		this.maxDelay = requirePositive(maxDelay, "maxDelay");
		if (initialDelay.compareTo(maxDelay) > 0) {
			throw new IllegalArgumentException("initialDelay must not exceed maxDelay");
		}
		this.jitterSource = Objects.requireNonNull(jitterSource, "jitterSource must not be null");
	}

	public Duration delay(int attemptCount) {
		if (attemptCount < 1) {
			throw new IllegalArgumentException("attemptCount must be positive");
		}
		long cappedMillis = initialDelay.toMillis();
		long maximumMillis = maxDelay.toMillis();
		for (int index = 1; index < attemptCount && cappedMillis < maximumMillis; index++) {
			cappedMillis = Math.min(maximumMillis, cappedMillis > maximumMillis / 2
					? maximumMillis
					: cappedMillis * 2);
		}
		double jitter = jitterSource.getAsDouble();
		if (jitter < 0.0d || jitter >= 1.0d || Double.isNaN(jitter)) {
			throw new IllegalStateException("jitterSource must return a value in [0, 1)");
		}
		long jitteredMillis = Math.round(cappedMillis * (MIN_JITTER_FACTOR + JITTER_RANGE * jitter));
		return Duration.ofMillis(Math.max(1L, Math.min(maximumMillis, jitteredMillis)));
	}

	private static Duration requirePositive(Duration value, String fieldName) {
		Duration required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isZero() || required.isNegative() || required.toMillis() < 1) {
			throw new IllegalArgumentException(fieldName + " must be at least one millisecond");
		}
		return required;
	}
}
