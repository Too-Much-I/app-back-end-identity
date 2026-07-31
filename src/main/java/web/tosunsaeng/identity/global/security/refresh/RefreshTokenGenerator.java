package web.tosunsaeng.identity.global.security.refresh;

import java.security.SecureRandom;
import java.util.Base64;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenGenerator {

	private final RefreshTokenProperties properties;
	private final SecureRandom secureRandom;

	public String generate() {
		byte[] randomValue = new byte[properties.randomBytes()];
		secureRandom.nextBytes(randomValue);
		// 전송에 안전한 패딩 없는 URL-safe 난수 문자열을 만든다.
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomValue);
	}
}
