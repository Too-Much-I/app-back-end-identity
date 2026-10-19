package web.tosunsaeng.identity.domain.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class UserWithdrawalIdentityReleasePropertiesTests {

	@Test
	void disabledConfigurationAcceptsSafeDefaults() {
		assertThatCode(new UserWithdrawalIdentityReleaseProperties()::validate)
				.doesNotThrowAnyException();
	}

	@Test
	void enabledConfigurationRejectsInvalidDelayAndBatch() {
		UserWithdrawalIdentityReleaseProperties properties =
				new UserWithdrawalIdentityReleaseProperties();
		properties.setEnabled(true);
		properties.setFixedDelay(Duration.ZERO);

		assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);

		properties.setFixedDelay(Duration.ofSeconds(1));
		properties.setMaxBatchSize(101);
		assertThatThrownBy(properties::validate).isInstanceOf(IllegalArgumentException.class);
	}
}
