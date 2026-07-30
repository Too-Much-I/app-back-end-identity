package web.tosunsaeng.identity.global.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class RefreshTokenGeneratorTests {

	@Test
	void generatesUrlSafeUnpaddedOpaqueTokenFromThirtyTwoRandomBytes() {
		RefreshTokenGenerator generator = new RefreshTokenGenerator(
				new RefreshTokenProperties(Duration.ofDays(14), 32),
				new SecureRandom()
		);

		String tokenValue = generator.generate();

		assertThat(tokenValue)
				.hasSize(43)
				.matches("[A-Za-z0-9_-]+")
				.doesNotContain("=", ".");
	}

	@Test
	void rejectsRandomByteLengthBelowThirtyTwo() {
		assertThatThrownBy(() -> new RefreshTokenProperties(Duration.ofDays(14), 31))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("app.refresh-token.random-bytes must be at least 32");
	}
}
