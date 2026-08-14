package web.tosunsaeng.identity.domain.auth.infrastructure.phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class PhoneFingerprintPropertiesTests {

	@Test
	void parsesOneActiveAndLookupOnlyKeysWithoutExposingConfiguration() {
		String keyRing = "v1,LOOKUP_ONLY," + encodedKey(1)
				+ ";v2,ACTIVE_WRITE," + encodedKey(2);
		PhoneFingerprintProperties properties = new PhoneFingerprintProperties(true, keyRing);

		assertThat(properties.keyRegistry().activeWriteKey().version()).isEqualTo("v2");
		assertThat(properties.keyRegistry().retainedKeys()).hasSize(2);
		assertThat(properties.toString()).doesNotContain(keyRing).contains("[REDACTED]");
	}

	@Test
	void disabledOrInvalidConfigurationFailsClosedWithoutEchoingKeyRing() {
		String material = encodedKey(3);
		assertThatThrownBy(() -> new PhoneFingerprintProperties(false, "").keyRegistry())
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("disabled");
		assertThatThrownBy(() -> new PhoneFingerprintProperties(
				true,
				"v1,LOOKUP_ONLY," + material
		).keyRegistry())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining(material);
		assertThatThrownBy(() -> new PhoneFingerprintProperties(
				true,
				"v1,ACTIVE_WRITE," + material + ";broken"
		).keyRegistry())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining(material);
	}

	private String encodedKey(int fill) {
		byte[] material = new byte[32];
		Arrays.fill(material, (byte) fill);
		return Base64.getEncoder().encodeToString(material);
	}
}
