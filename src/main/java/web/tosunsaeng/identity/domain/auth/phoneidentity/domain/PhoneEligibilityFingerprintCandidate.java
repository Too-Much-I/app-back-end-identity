package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import java.util.Objects;

public record PhoneEligibilityFingerprintCandidate(
		String keyVersion,
		String fingerprint
) {

	public PhoneEligibilityFingerprintCandidate {
		keyVersion = requireToken(keyVersion, "keyVersion");
		fingerprint = requireToken(fingerprint, "fingerprint");
	}

	private static String requireToken(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank() || required.chars().anyMatch(Character::isWhitespace)) {
			throw new IllegalArgumentException(fieldName + " must be a non-blank token");
		}
		return required;
	}

	@Override
	public String toString() {
		return "PhoneEligibilityFingerprintCandidate[keyVersion=" + keyVersion
				+ ", fingerprint=[REDACTED]]";
	}
}
