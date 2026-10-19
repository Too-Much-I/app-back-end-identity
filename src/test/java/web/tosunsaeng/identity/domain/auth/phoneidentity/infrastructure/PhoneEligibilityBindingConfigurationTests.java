package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;

class PhoneEligibilityBindingConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PhoneEligibilityBindingConfiguration.class);

	@Test
	void disabledByDefaultWithoutCreatingHasher() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(PhoneEligibilityBindingProperties.class);
			assertThat(context).doesNotHaveBean(PhoneEligibilityFingerprintHasher.class);
		});
	}

	@Test
	void enabledConfigurationRequiresSeparateScopeAndKeyRing() {
		contextRunner
				.withPropertyValues("app.phone-eligibility-binding.enabled=true")
				.run(context -> assertThat(context.getStartupFailure())
						.isNotNull()
						.hasStackTraceContaining(
								"Phone eligibility binding configuration is invalid"
						));
	}

	@Test
	void enabledConfigurationCreatesDedicatedHasherAndRedactsKey() {
		String key = Base64.getEncoder().encodeToString(new byte[32]);
		contextRunner
				.withPropertyValues(
						"app.phone-eligibility-binding.enabled=true",
						"app.phone-eligibility-binding.consumer-scope-id=opaque-scope-v1",
						"app.phone-eligibility-binding.key-ring=v1,ACTIVE_WRITE," + key
				)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(PhoneEligibilityFingerprintHasher.class);
					assertThat(context.getBean(PhoneEligibilityBindingProperties.class).toString())
							.doesNotContain(key);
				});
	}
}
