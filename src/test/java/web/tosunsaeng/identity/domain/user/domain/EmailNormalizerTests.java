package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.api.Test;

class EmailNormalizerTests {

	private final EmailNormalizer emailNormalizer = new EmailNormalizer();

	@Test
	void trimsLeadingAndTrailingWhitespace() {
		String normalizedEmail = emailNormalizer.normalize("  Sample.User@example.test  ");

		assertThat(normalizedEmail).isEqualTo("sample.user@example.test");
	}

	@Test
	void lowercasesEmailUsingLocaleRoot() {
		String normalizedEmail = emailNormalizer.normalize("I@EXAMPLE.TEST");

		assertThat(normalizedEmail).isEqualTo("i@example.test");
	}

	@Test
	void differentEmailCasingHasSameNormalizedValue() {
		String first = emailNormalizer.normalize("Sample.User@Example.Test");
		String second = emailNormalizer.normalize("sample.user@example.test");

		assertThat(first).isEqualTo(second);
	}

	@Test
	void doesNotApplyProviderSpecificTransformations() {
		String normalizedEmail = emailNormalizer.normalize("First.Last+tag@GMAIL.COM");

		assertThat(normalizedEmail).isEqualTo("first.last+tag@gmail.com");
	}

	@Test
	void rejectsNullEmail() {
		assertThatNullPointerException()
				.isThrownBy(() -> emailNormalizer.normalize(null));
	}
}
