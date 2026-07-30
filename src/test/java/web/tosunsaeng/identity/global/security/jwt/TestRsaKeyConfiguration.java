package web.tosunsaeng.identity.global.security.jwt;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class TestRsaKeyConfiguration {

	public static final Instant TEST_INSTANT = Instant.parse("2026-07-24T01:02:03Z");
	private static final RsaKeyMaterial KEY_MATERIAL = generateKeyMaterial();

	@Bean
	@Primary
	RsaKeyMaterial testRsaKeyMaterial() {
		return KEY_MATERIAL;
	}

	@Bean
	@Primary
	Clock testClock() {
		return Clock.fixed(TEST_INSTANT, ZoneOffset.UTC);
	}

	private static RsaKeyMaterial generateKeyMaterial() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			KeyPair keyPair = generator.generateKeyPair();
			return new RsaKeyMaterial(
					(RSAPrivateKey) keyPair.getPrivate(),
					(RSAPublicKey) keyPair.getPublic()
			);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA is not available in the test runtime.");
		}
	}
}
