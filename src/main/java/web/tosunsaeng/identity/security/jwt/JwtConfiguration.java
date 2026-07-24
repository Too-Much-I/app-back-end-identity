package web.tosunsaeng.identity.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	@Lazy
	public RsaKeyMaterial rsaKeyMaterial(
			RsaKeyLoader rsaKeyLoader,
			JwtProperties properties
	) {
		return rsaKeyLoader.load(
				properties.privateKeyLocation(),
				properties.publicKeyLocation()
		);
	}

	@Bean
	public RSAPrivateKey rsaPrivateKey(RsaKeyMaterial keyMaterial) {
		return keyMaterial.privateKey();
	}

	@Bean
	public RSAPublicKey rsaPublicKey(RsaKeyMaterial keyMaterial) {
		return keyMaterial.publicKey();
	}

	@Bean
	public RSAKey rsaKey(
			RSAPublicKey publicKey,
			RSAPrivateKey privateKey,
			JwtProperties properties
	) {
		return new RSAKey.Builder(publicKey)
				.privateKey(privateKey)
				.keyID(properties.keyId())
				.algorithm(JWSAlgorithm.RS256)
				.keyUse(KeyUse.SIGNATURE)
				.build();
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (selector, context) -> selector.select(jwkSet);
	}

	@Bean
	public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}
}
