package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.DoubleSupplier;

public final class UserWithdrawnRetryPolicy {

	private static final Duration BASE = Duration.ofSeconds(5);
	private static final Duration CAP = Duration.ofMinutes(15);
	private final DoubleSupplier jitterSource;

	public UserWithdrawnRetryPolicy(DoubleSupplier jitterSource) {
		this.jitterSource = Objects.requireNonNull(jitterSource);
	}

	public Duration delay(int attemptCount) {
		if (attemptCount < 1) {
			throw new IllegalArgumentException("attemptCount must be positive");
		}
		long multiplier = 1L << Math.min(attemptCount - 1, 20);
		long cappedMillis = Math.min(
				Math.multiplyExact(BASE.toMillis(), multiplier),
				CAP.toMillis()
		);
		double sample = jitterSource.getAsDouble();
		if (sample < 0.0 || sample > 1.0) {
			throw new IllegalStateException("jitter source must return a value from 0 to 1");
		}
		return Duration.ofMillis(Math.round(cappedMillis * (0.8 + sample * 0.4)));
	}
}
