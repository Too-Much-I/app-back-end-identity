package web.tosunsaeng.identity.global.security.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RefreshTokenHasherTests {

	private final RefreshTokenHasher hasher = new RefreshTokenHasher();

	@Test
	void createsTheSameSha256HashForTheSameInput() {
		String firstHash = hasher.hash("same-test-input");
		String secondHash = hasher.hash("same-test-input");

		assertThat(firstHash)
				.isEqualTo(secondHash)
				.hasSize(43)
				.matches("[A-Za-z0-9_-]+")
				.doesNotContain("=");
	}

	@Test
	void createsDifferentSha256HashesForDifferentInputs() {
		assertThat(hasher.hash("first-test-input"))
				.isNotEqualTo(hasher.hash("second-test-input"));
	}
}
