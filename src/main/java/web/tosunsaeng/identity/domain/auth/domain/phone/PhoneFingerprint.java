package web.tosunsaeng.identity.domain.auth.domain.phone;

import java.util.Objects;

public final class PhoneFingerprint {

	private static final String VERSION_PATTERN = "[A-Za-z0-9._-]{1,32}";
	private static final String FINGERPRINT_PATTERN = "[A-Za-z0-9_-]{43}";

	private final String keyVersion;
	private final String value;

	public PhoneFingerprint(String keyVersion, String value) {
		this.keyVersion = requireMatch(keyVersion, VERSION_PATTERN, "key version");
		this.value = requireMatch(value, FINGERPRINT_PATTERN, "fingerprint");
	}

	private static String requireMatch(String value, String pattern, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (!required.matches(pattern)) {
			throw new IllegalArgumentException("Phone " + fieldName + " is invalid.");
		}
		return required;
	}

	public String keyVersion() {
		return keyVersion;
	}

	public String value() {
		return value;
	}

	@Override
	public String toString() {
		return "PhoneFingerprint[keyVersion=" + keyVersion + ", value=[REDACTED]]";
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PhoneFingerprint that)) {
			return false;
		}
		return keyVersion.equals(that.keyVersion) && value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(keyVersion, value);
	}
}
