package web.tosunsaeng.identity.domain.auth.federation.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class FirebaseAbandonedCleanupPropertiesTests {

	@Test
	void defaultsAreDisabledAndRetainLifecycleLongerThanAttempts() {
		FirebaseAbandonedCleanupProperties properties =
				new FirebaseAbandonedCleanupProperties();

		properties.validate();

		assertThat(properties.isCaptureEnabled()).isFalse();
		assertThat(properties.isWorkerEnabled()).isFalse();
		assertThat(properties.getGrace()).isEqualTo(Duration.ofHours(24));
		assertThat(properties.getTerminalLifecycleRetention())
				.isGreaterThan(properties.getTerminalEnrollmentRetention());
	}

	@Test
	void lifecycleMustOutliveTerminalAttempt() {
		FirebaseAbandonedCleanupProperties properties =
				new FirebaseAbandonedCleanupProperties();
		properties.setTerminalLifecycleRetention(Duration.ofHours(1));
		properties.setTerminalEnrollmentRetention(Duration.ofHours(1));

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);
	}
}
