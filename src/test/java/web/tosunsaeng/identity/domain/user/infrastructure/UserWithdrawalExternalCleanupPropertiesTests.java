package web.tosunsaeng.identity.domain.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;

class UserWithdrawalExternalCleanupPropertiesTests {

	@Test
	void defaultsAreDisabledAndSafe() {
		UserWithdrawalExternalCleanupProperties properties =
				new UserWithdrawalExternalCleanupProperties();

		properties.validate(null);

		assertThat(properties.isEnabled()).isFalse();
		assertThat(properties.getMaxBatchSize()).isEqualTo(20);
		assertThat(properties.toString()).doesNotContain("project", "credential", "token");
	}

	@Test
	void acceptsEnabledCleanupOnlyWithEnabledFirebaseAndSufficientLease() {
		UserWithdrawalExternalCleanupProperties properties = enabledProperties();

		properties.validate(firebaseProperties(true));
	}

	@Test
	void rejectsDisabledFirebaseInvalidBatchAndLeaseShorterThanCallBudget() {
		UserWithdrawalExternalCleanupProperties properties = enabledProperties();
		assertThatThrownBy(() -> properties.validate(firebaseProperties(false)))
				.isInstanceOf(IllegalArgumentException.class);

		properties.setMaxBatchSize(101);
		assertThatThrownBy(() -> properties.validate(firebaseProperties(true)))
				.isInstanceOf(IllegalArgumentException.class);

		properties.setMaxBatchSize(20);
		properties.setLeaseDuration(Duration.ofSeconds(8));
		assertThatThrownBy(() -> properties.validate(firebaseProperties(true)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private UserWithdrawalExternalCleanupProperties enabledProperties() {
		UserWithdrawalExternalCleanupProperties properties =
				new UserWithdrawalExternalCleanupProperties();
		properties.setEnabled(true);
		return properties;
	}

	private FirebaseAuthProperties firebaseProperties(boolean enabled) {
		return new FirebaseAuthProperties(
				enabled,
				enabled ? "test-project" : null,
				null,
				true,
				false,
				false,
				true,
				"oidc.kakao",
				Duration.ofMinutes(15),
				Duration.ofMinutes(5),
				Duration.ofSeconds(30),
				Duration.ofSeconds(3),
				Duration.ofSeconds(5),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}
}
