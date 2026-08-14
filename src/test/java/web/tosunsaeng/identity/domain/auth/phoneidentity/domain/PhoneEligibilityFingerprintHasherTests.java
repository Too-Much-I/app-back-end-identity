package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;

class PhoneEligibilityFingerprintHasherTests {

	@Test
	void derivesConsumerScopedCandidatesDifferentFromPhoneIdentityFingerprint() {
		byte[] key = new byte[32];
		java.util.Arrays.fill(key, (byte) 7);
		PhoneFingerprintKeyRegistry registry = new PhoneFingerprintKeyRegistry(List.of(
				PhoneFingerprintKey.fromBase64(
						"v1",
						PhoneFingerprintKeyStatus.ACTIVE_WRITE,
						Base64.getEncoder().encodeToString(key)
				)
		));
		PhoneEligibilityFingerprintHasher eligibility =
				new PhoneEligibilityFingerprintHasher("consumer-a", registry);
		PhoneEligibilityFingerprintHasher otherScope =
				new PhoneEligibilityFingerprintHasher("consumer-b", registry);
		PhoneFingerprintHasher identity = new PhoneFingerprintHasher(registry);

		PhoneEligibilityFingerprintCandidate candidate = eligibility
				.fingerprint("+821012345678")
				.getFirst();

		assertThat(candidate.keyVersion()).isEqualTo("v1");
		assertThat(candidate.fingerprint())
				.isNotEqualTo(identity.fingerprint("+821012345678").activeWrite().value())
				.isNotEqualTo(otherScope.fingerprint("+821012345678").getFirst().fingerprint());
		assertThat(candidate.toString()).doesNotContain(candidate.fingerprint());
		assertThat(eligibility.toString()).doesNotContain(Base64.getEncoder().encodeToString(key));
	}

	@Test
	void rejectsNonCanonicalPhoneAndUnsafeConsumerScope() {
		PhoneFingerprintKeyRegistry registry = registry();

		assertThatThrownBy(() -> new PhoneEligibilityFingerprintHasher("unsafe scope", registry))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new PhoneEligibilityFingerprintHasher("consumer-a", registry)
				.fingerprint("010-1234-5678"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private PhoneFingerprintKeyRegistry registry() {
		return new PhoneFingerprintKeyRegistry(List.of(PhoneFingerprintKey.fromBase64(
				"v1",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				Base64.getEncoder().encodeToString(new byte[32])
		)));
	}
}
