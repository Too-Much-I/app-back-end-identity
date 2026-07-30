package web.tosunsaeng.identity.global.security.refresh;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

	private static final String HASH_ALGORITHM = "SHA-256";

	public String hash(String tokenValue) {
		String requiredTokenValue = Objects.requireNonNull(
				tokenValue,
				"tokenValue must not be null"
		);
		try {
			MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
			byte[] tokenHash = digest.digest(requiredTokenValue.getBytes(StandardCharsets.UTF_8));
			return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenHash);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available in the runtime.");
		}
	}
}
