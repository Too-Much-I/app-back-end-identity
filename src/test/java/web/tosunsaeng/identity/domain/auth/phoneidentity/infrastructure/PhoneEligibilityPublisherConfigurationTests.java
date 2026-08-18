package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.phoneidentity.application.PhoneEligibilityBindingPublisher;

class PhoneEligibilityPublisherConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(PhoneEligibilityPublisherConfiguration.class);

	@Test
	void publisherIsDisabledByDefaultWithoutDeliveryOrSchedulerBeans() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).doesNotHaveBean(PhoneEligibilityBindingPublisher.class);
			assertThat(context).doesNotHaveBean(PhoneEligibilityBindingPublisherScheduler.class);
			assertThat(context).doesNotHaveBean(WorkloadIdentityCredentialProvider.class);
		});
	}

	@Test
	void enabledPropertiesFailClosedForNonHttpsOrMissingAudience() {
		PhoneEligibilityPublisherProperties properties = new PhoneEligibilityPublisherProperties();
		properties.setEnabled(true);
		properties.setEndpoint(URI.create("http://consumer.test/events"));
		properties.setAudience("");
		PhoneEligibilityBindingProperties binding = new PhoneEligibilityBindingProperties(
				true, "opaque-scope-v1", "not-inspected-by-publisher-validation");

		assertThatThrownBy(() -> properties.validate(binding))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Phone eligibility publisher configuration is invalid.");
	}

	@Test
	void workloadCredentialLimitsLifetimeAndRedactsToken() {
		Instant issuedAt = Instant.parse("2026-08-14T00:00:00Z");
		WorkloadIdentityCredential credential = new WorkloadIdentityCredential(
				"sensitive-workload-token", issuedAt, issuedAt.plus(Duration.ofMinutes(5)));
		assertThat(credential.toString()).doesNotContain("sensitive-workload-token");
		assertThatThrownBy(() -> new WorkloadIdentityCredential(
				"another-sensitive-token", issuedAt, issuedAt.plusSeconds(301)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
