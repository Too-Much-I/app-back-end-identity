package web.tosunsaeng.identity.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

class JwksRotationTests {

	@Test
	@SuppressWarnings("unchecked")
	void exposesActiveAndPreviousPublicKeysWithoutPrivateMaterial() throws Exception {
		KeyPair activePair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		KeyPair previousPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		JwtProperties jwtProperties = jwtProperties();
		JwtConfiguration configuration = new JwtConfiguration();
		RSAKey activeKey = configuration.rsaKey(
				(RSAPublicKey) activePair.getPublic(),
				(RSAPrivateKey) activePair.getPrivate(),
				jwtProperties
		);
		RsaKeyLoader loader = mock(RsaKeyLoader.class);
		when(loader.loadPublicKey("classpath:previous.pem"))
				.thenReturn((RSAPublicKey) previousPair.getPublic());
		JwksRotationProperties rotation = new JwksRotationProperties();
		rotation.setPreviousKeyIds("identity-previous-key");
		rotation.setPreviousPublicKeyLocations("classpath:previous.pem");

		Map<String, Object> result = configuration.jwksPublicKeySet(
				activeKey,
				loader,
				jwtProperties,
				rotation
		).toJsonObject();
		List<Map<String, Object>> keys = (List<Map<String, Object>>) result.get("keys");

		assertThat(keys).extracting(key -> key.get("kid"))
				.containsExactly("identity-active-key", "identity-previous-key");
		assertThat(keys).allSatisfy(key -> assertThat(key)
				.doesNotContainKeys("d", "p", "q", "dp", "dq", "qi", "oth"));
	}

	@Test
	void rejectsMismatchedOrDuplicateRotationConfiguration() {
		JwksRotationProperties mismatched = new JwksRotationProperties();
		mismatched.setPreviousKeyIds("old-1,old-2");
		mismatched.setPreviousPublicKeyLocations("classpath:old-1.pem");
		assertThatThrownBy(() -> mismatched.validate("active"))
				.isInstanceOf(IllegalArgumentException.class);

		JwksRotationProperties activeRepeated = new JwksRotationProperties();
		activeRepeated.setPreviousKeyIds("active");
		activeRepeated.setPreviousPublicKeyLocations("classpath:old.pem");
		assertThatThrownBy(() -> activeRepeated.validate("active"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private JwtProperties jwtProperties() {
		return new JwtProperties(
				"https://identity.test",
				"tosunsaeng-learning-core",
				"identity-active-key",
				Duration.ofMinutes(30),
				"unused-private",
				"unused-public",
				List.of("learning:read")
		);
	}
}
