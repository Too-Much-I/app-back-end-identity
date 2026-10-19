package web.tosunsaeng.identity.domain.auth.usermerge.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import web.tosunsaeng.identity.domain.auth.usermerge.application.UserMergedPublisher;

class UserMergedPublisherConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(UserMergedPublisherConfiguration.class);

	@Test
	void publisherIsDisabledByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(UserMergedPublisherProperties.class);
			assertThat(context.getBean(UserMergedPublisherProperties.class).isEnabled())
					.isFalse();
			assertThat(context).doesNotHaveBean(UserMergedPublisher.class);
			assertThat(context).doesNotHaveBean(JdkUserMergedDeliveryAdapter.class);
		});
	}

	@Test
	void enabledPublisherRequiresExactSafeHttpsEndpoint() throws Exception {
		UserMergedPublisherProperties properties = new UserMergedPublisherProperties();
		properties.setEnabled(true);
		properties.setEndpoint(new URI("http://learning-core.test/internal/user-merged"));

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);

		properties.setEndpoint(new URI(
				"https://learning-core.test/internal/v1/events/user-merged"));
		properties.validate();

		properties.setEndpoint(new URI(
				"https://learning-core.test/internal/v1/events/user-merged?unexpected=true"));
		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalArgumentException.class);
	}
}
