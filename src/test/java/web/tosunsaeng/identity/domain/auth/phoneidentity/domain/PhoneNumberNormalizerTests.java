package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PhoneNumberNormalizerTests {

	private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

	@Test
	void normalizesEquivalentE164Formatting() {
		assertThat(normalizer.normalize("+1 (415) 555-2671"))
				.isEqualTo("+14155552671");
		assertThat(normalizer.normalize("  +1.415.555.2671  "))
				.isEqualTo("+14155552671");
		assertThat(normalizer.normalize("+82 10-1234-5678"))
				.isEqualTo("+821012345678");
	}

	@Test
	void rejectsNonE164OrAmbiguousInputWithoutEchoingIt() {
		String[] invalid = {
				"010-1234-5678",
				"+0123456789",
				"+1234567",
				"+1234567890123456",
				"+1 415 555 2671 ext 9",
				"++14155552671",
				"+١٤١٥٥٥٥٢٦٧١"
		};
		for (String value : invalid) {
			assertThatThrownBy(() -> normalizer.normalize(value))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessage("Verified phone number is not valid E.164 input.")
					.hasMessageNotContaining(value);
		}
	}
}
