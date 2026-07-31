package web.tosunsaeng.identity.global.security.guest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class GuestInstallationIdHasher {

	private static final String HASH_ALGORITHM = "SHA-256";
	private static final int CANONICAL_UUID_LENGTH = 36;

	public String hash(String installationId) {
		String normalizedInstallationId = normalize(installationId);
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] hash = digest.digest(
					normalizedInstallationId.getBytes(StandardCharsets.UTF_8)
			);
			return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available in the runtime.");
		}
	}

	String normalize(String installationId) {
		if (installationId == null) {
			throw invalidInstallationId();
		}
		String trimmed = installationId.trim();
		if (trimmed.length() != CANONICAL_UUID_LENGTH) {
			throw invalidInstallationId();
		}
		try {
			UUID uuid = UUID.fromString(trimmed);
			if (!uuid.toString().equalsIgnoreCase(trimmed)
					|| uuid.version() != 4
					|| uuid.variant() != 2) {
				throw invalidInstallationId();
			}
			return uuid.toString();
		} catch (IllegalArgumentException exception) {
			throw invalidInstallationId();
		}
	}

	private IllegalArgumentException invalidInstallationId() {
		return new IllegalArgumentException("Guest installation ID must be a canonical UUID v4.");
	}
}
