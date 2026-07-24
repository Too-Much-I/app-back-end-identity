package web.tosunsaeng.identity.security.refresh;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenGenerator {

	private final RefreshTokenProperties properties;
	private final SecureRandom secureRandom;

	public RefreshTokenGenerator(
			RefreshTokenProperties properties,
			SecureRandom secureRandom
	) {
		this.properties = properties;
		this.secureRandom = secureRandom;
	}

	public String generate() {
		byte[] randomValue = new byte[properties.randomBytes()];
		secureRandom.nextBytes(randomValue);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomValue);
	}
}
