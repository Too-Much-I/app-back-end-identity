package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class OwnerEventRetryPolicy {
	private final Duration initial;
	private final Duration maximum;
	private final DoubleSupplier jitter;

	public OwnerEventRetryPolicy(Duration initial, Duration maximum, DoubleSupplier jitter) {
		this.initial = positive(initial);
		this.maximum = positive(maximum);
		if (initial.compareTo(maximum) > 0) throw new IllegalArgumentException("initial exceeds maximum");
		this.jitter = Objects.requireNonNull(jitter);
	}

	public Duration delay(int attemptCount) {
		if (attemptCount < 1) throw new IllegalArgumentException("attemptCount must be positive");
		long factor = 1L << Math.min(attemptCount - 1, 20);
		long capped = Math.min(Math.multiplyExact(initial.toMillis(), factor), maximum.toMillis());
		double sample = jitter.getAsDouble();
		if (sample < 0 || sample > 1) throw new IllegalStateException("invalid jitter sample");
		return Duration.ofMillis(Math.round(capped * (0.8 + sample * 0.4)));
	}

	private static Duration positive(Duration value) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException("duration must be positive");
		}
		return value;
	}
}
