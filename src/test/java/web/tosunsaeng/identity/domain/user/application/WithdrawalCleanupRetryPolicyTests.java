package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class WithdrawalCleanupRetryPolicyTests {

	@Test
	void appliesBoundedExponentialBackoffWithInjectableJitter() {
		WithdrawalCleanupRetryPolicy policy = new WithdrawalCleanupRetryPolicy(
				Duration.ofSeconds(5), Duration.ofSeconds(20), () -> 0.5d
		);

		assertThat(policy.delay(1)).isEqualTo(Duration.ofSeconds(5));
		assertThat(policy.delay(2)).isEqualTo(Duration.ofSeconds(10));
		assertThat(policy.delay(3)).isEqualTo(Duration.ofSeconds(20));
		assertThat(policy.delay(10)).isEqualTo(Duration.ofSeconds(20));
	}

	@Test
	void rejectsInvalidDurationsAttemptsAndJitter() {
		assertThatThrownBy(() -> new WithdrawalCleanupRetryPolicy(
				Duration.ofSeconds(2), Duration.ofSeconds(1), () -> 0.5d
		)).isInstanceOf(IllegalArgumentException.class);
		WithdrawalCleanupRetryPolicy policy = new WithdrawalCleanupRetryPolicy(
				Duration.ofSeconds(1), Duration.ofSeconds(2), () -> 1.0d
		);
		assertThatThrownBy(() -> policy.delay(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> policy.delay(1)).isInstanceOf(IllegalStateException.class);
	}
}
