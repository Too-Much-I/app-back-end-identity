package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;

class PhoneFingerprintTests {

	private static final String NORMALIZED_PHONE = "+14155552671";

	@Test
	void registryRequiresExactlyOneActiveWriteAndUniqueVersions() {
		PhoneFingerprintKey lookup = key("v1", PhoneFingerprintKeyStatus.LOOKUP_ONLY, 1);
		PhoneFingerprintKey active = key("v2", PhoneFingerprintKeyStatus.ACTIVE_WRITE, 2);

		assertThatThrownBy(() -> new PhoneFingerprintKeyRegistry(List.of(lookup)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Exactly one active");
		assertThatThrownBy(() -> new PhoneFingerprintKeyRegistry(List.of(
				active,
				key("v3", PhoneFingerprintKeyStatus.ACTIVE_WRITE, 3)
		)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Exactly one active");
		assertThatThrownBy(() -> new PhoneFingerprintKeyRegistry(List.of(
				active,
				key("v2", PhoneFingerprintKeyStatus.LOOKUP_ONLY, 4)
		)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("versions must be unique");
	}

	@Test
	void rejectsShortOrMalformedKeyWithoutExposingMaterial() {
		String shortMaterial = Base64.getEncoder().encodeToString(new byte[16]);
		assertThatThrownBy(() -> PhoneFingerprintKey.fromBase64(
				"v1",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				shortMaterial
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining(shortMaterial);
		assertThatThrownBy(() -> PhoneFingerprintKey.fromBase64(
				"v1",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				"not-base64***"
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining("not-base64");
	}

	@Test
	void generatesDeterministicDomainSeparatedFingerprintsForEveryRetainedVersion()
			throws Exception {
		PhoneFingerprintKey oldKey = key(
				"v1",
				PhoneFingerprintKeyStatus.LOOKUP_ONLY,
				1
		);
		PhoneFingerprintKey activeKey = key(
				"v2",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				2
		);
		PhoneFingerprintHasher hasher = new PhoneFingerprintHasher(
				new PhoneFingerprintKeyRegistry(List.of(oldKey, activeKey))
		);

		PhoneFingerprintSet first = hasher.fingerprint(NORMALIZED_PHONE);
		PhoneFingerprintSet second = hasher.fingerprint(NORMALIZED_PHONE);

		assertThat(first.activeWrite().keyVersion()).isEqualTo("v2");
		assertThat(first.retained()).containsExactlyElementsOf(second.retained());
		assertThat(first.retained())
				.extracting(PhoneFingerprint::keyVersion)
				.containsExactly("v1", "v2");
		assertThat(first.retained())
				.extracting(PhoneFingerprint::value)
				.doesNotHaveDuplicates();

		Mac phoneOnlyMac = Mac.getInstance("HmacSHA256");
		phoneOnlyMac.init(new SecretKeySpec(activeKey.copyKeyMaterial(), "HmacSHA256"));
		String phoneOnly = Base64.getUrlEncoder().withoutPadding().encodeToString(
				phoneOnlyMac.doFinal(NORMALIZED_PHONE.getBytes(StandardCharsets.US_ASCII))
		);
		assertThat(first.activeWrite().value()).isNotEqualTo(phoneOnly);
	}

	@Test
	void rejectsUnnormalizedHasherInputAndRedactsSensitiveStringRepresentations() {
		PhoneFingerprintKey key = key("v1", PhoneFingerprintKeyStatus.ACTIVE_WRITE, 7);
		PhoneFingerprintHasher hasher = new PhoneFingerprintHasher(
				new PhoneFingerprintKeyRegistry(List.of(key))
		);
		PhoneFingerprintSet fingerprints = hasher.fingerprint(NORMALIZED_PHONE);

		assertThatThrownBy(() -> hasher.fingerprint("+1 415 555 2671"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining("415");
		assertThat(key.toString()).doesNotContain(encodedKey(7));
		assertThat(fingerprints.activeWrite().toString())
				.doesNotContain(fingerprints.activeWrite().value());
		assertThat(fingerprints.toString()).doesNotContain(NORMALIZED_PHONE);
		assertThat(hasher.toString()).doesNotContain(encodedKey(7));
	}

	private PhoneFingerprintKey key(
			String version,
			PhoneFingerprintKeyStatus status,
			int fill
	) {
		return PhoneFingerprintKey.fromBase64(version, status, encodedKey(fill));
	}

	private String encodedKey(int fill) {
		byte[] material = new byte[32];
		java.util.Arrays.fill(material, (byte) fill);
		return Base64.getEncoder().encodeToString(material);
	}
}
