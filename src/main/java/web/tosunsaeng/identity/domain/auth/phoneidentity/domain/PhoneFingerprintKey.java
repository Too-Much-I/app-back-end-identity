package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;

public final class PhoneFingerprintKey {

	private static final int MINIMUM_KEY_BYTES = 32;
	private static final String VERSION_PATTERN = "[A-Za-z0-9._-]{1,32}";

	private final String version;
	private final PhoneFingerprintKeyStatus status;
	private final byte[] keyMaterial;

	private PhoneFingerprintKey(
			String version,
			PhoneFingerprintKeyStatus status,
			byte[] keyMaterial
	) {
		this.version = requireVersion(version);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.keyMaterial = requireKeyMaterial(keyMaterial);
	}

	public static PhoneFingerprintKey fromBase64(
			String version,
			PhoneFingerprintKeyStatus status,
			String base64KeyMaterial
	) {
		try {
			return new PhoneFingerprintKey(
					version,
					status,
					Base64.getDecoder().decode(Objects.requireNonNull(
							base64KeyMaterial,
							"base64KeyMaterial must not be null"
					))
			);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Phone fingerprint key configuration is invalid.");
		}
	}

	private static String requireVersion(String value) {
		String required = Objects.requireNonNull(value, "version must not be null");
		if (!required.matches(VERSION_PATTERN)) {
			throw new IllegalArgumentException("Phone fingerprint key version is invalid.");
		}
		return required;
	}

	private static byte[] requireKeyMaterial(byte[] value) {
		byte[] required = Objects.requireNonNull(value, "keyMaterial must not be null").clone();
		if (required.length < MINIMUM_KEY_BYTES) {
			throw new IllegalArgumentException("Phone fingerprint key is too short.");
		}
		return required;
	}

	public String version() {
		return version;
	}

	public PhoneFingerprintKeyStatus status() {
		return status;
	}

	byte[] copyKeyMaterial() {
		return keyMaterial.clone();
	}

	@Override
	public String toString() {
		return "PhoneFingerprintKey[version=" + version
				+ ", status=" + status + ", keyMaterial=[REDACTED]]";
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof PhoneFingerprintKey that)) {
			return false;
		}
		return version.equals(that.version)
				&& status == that.status
				&& Arrays.equals(keyMaterial, that.keyMaterial);
	}

	@Override
	public int hashCode() {
		return 31 * Objects.hash(version, status) + Arrays.hashCode(keyMaterial);
	}
}
