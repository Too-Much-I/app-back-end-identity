package web.tosunsaeng.identity.domain.user.withdrawalevent.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.Test;

class UserWithdrawnPublisherPropertiesTests {

	@Test
	void enabledPublisherRequiresExactHttpsEndpointAndAudience() {
		UserWithdrawnPublisherProperties valid = properties(
				"https://learning.test/internal/v1/events/withdrawn",
				"learning-core-user-withdrawn"
		);
		valid.validate();

		assertThatThrownBy(() -> properties(
				"http://learning.test/internal/v1/events/withdrawn",
				"learning-core-user-withdrawn"
		).validate()).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties(
				"https://learning.test/internal/v1/events/user-withdrawn",
				"learning-core-user-withdrawn"
		).validate()).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> properties(
				"https://learning.test/internal/v1/events/withdrawn",
				"another-audience"
		).validate()).isInstanceOf(IllegalArgumentException.class);
	}

	private UserWithdrawnPublisherProperties properties(String endpoint, String audience) {
		UserWithdrawnPublisherProperties properties = new UserWithdrawnPublisherProperties();
		properties.setEnabled(true);
		properties.setEndpoint(URI.create(endpoint));
		properties.setAudience(audience);
		return properties;
	}
}
