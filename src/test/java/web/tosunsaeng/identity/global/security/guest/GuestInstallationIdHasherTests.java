package web.tosunsaeng.identity.global.security.guest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class GuestInstallationIdHasherTests {

	private static final String INSTALLATION_ID = "550e8400-e29b-41d4-a716-446655440000";
	private final GuestInstallationIdHasher hasher = new GuestInstallationIdHasher();

	@Test
	void trimsCanonicalizesAndHashesInstallationIdWithSha256() {
		String normalizedHash = hasher.hash(INSTALLATION_ID);
		String whitespaceHash = hasher.hash("  " + INSTALLATION_ID.toUpperCase() + "  ");

		assertThat(whitespaceHash)
				.isEqualTo(normalizedHash)
				.hasSize(43)
				.matches("[A-Za-z0-9_-]+")
				.doesNotContain("=", INSTALLATION_ID);
	}

	@Test
	void differentInstallationIdsHaveDifferentHashes() {
		assertThat(hasher.hash(INSTALLATION_ID)).isNotEqualTo(
				hasher.hash("3f2504e0-4f89-41d3-9a0c-0305e82c3301")
		);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"   ",
			"not-a-uuid",
			"550e8400-e29b-11d4-a716-446655440000",
			"550e8400-e29b-41d4-7716-446655440000",
			"550e8400-e29b-41d4-a716-446655440000-extra"
	})
	void rejectsInvalidInstallationIdWithoutIncludingItInException(String invalidValue) {
		assertThatThrownBy(() -> hasher.hash(invalidValue))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Guest installation ID must be a canonical UUID v4.")
				.satisfies(exception -> {
					if (invalidValue != null && !invalidValue.isBlank()) {
						assertThat(exception.getMessage()).doesNotContain(invalidValue);
					}
				});
	}
}
