package web.tosunsaeng.identity.global.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({JwtProperties.class, JwksRotationProperties.class})
public class JwtConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public RsaKeyLoader rsaKeyLoader(ApplicationContext applicationContext) {
		// ApplicationContext를 사용해 ResourceLoader 타입의 Bean 주입 모호성을 피한다.
		return new RsaKeyLoader(applicationContext);
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
	public JwksPublicKeySet jwksPublicKeySet(
			RSAKey activeKey,
			RsaKeyLoader keyLoader,
			JwtProperties jwtProperties,
			JwksRotationProperties rotationProperties
	) {
		rotationProperties.validate(jwtProperties.keyId());
		List<com.nimbusds.jose.jwk.JWK> publicKeys = new ArrayList<>();
		publicKeys.add(activeKey.toPublicJWK());
		List<String> keyIds = rotationProperties.keyIds();
		List<String> locations = rotationProperties.publicKeyLocations();
		for (int index = 0; index < keyIds.size(); index++) {
			publicKeys.add(new RSAKey.Builder(keyLoader.loadPublicKey(locations.get(index)))
					.keyID(keyIds.get(index))
					.algorithm(JWSAlgorithm.RS256)
					.keyUse(KeyUse.SIGNATURE)
					.build());
		}
		return new JwksPublicKeySet(new JWKSet(publicKeys));
	}

	@Bean
	public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtDecoder jwtDecoder(
			JwksPublicKeySet publicKeys,
			JwtProperties properties,
			Clock clock
	) {
		DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
		processor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT));
		processor.setJWSKeySelector((header, context) -> publicKeys.verificationKeys(header));
		// Spring validators below own claims validation, including the injected clock.
		processor.setJWTClaimsSetVerifier((claims, context) -> { });
		NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);

		JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
		timestampValidator.setClock(clock);
		OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(properties.issuer());
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(
				properties.audience()
		);
		OAuth2TokenValidator<Jwt> subjectValidator = new JwtClaimValidator<String>(
				"sub",
				subject -> subject != null && !subject.isBlank()
		);
		OAuth2TokenValidator<Jwt> expirationValidator = new JwtClaimValidator<Instant>(
				"exp",
				expiresAt -> expiresAt != null
		);

		// kid는 위의 로컬 key selector에서 검증하고, 서명 검증 후 claim 계약을 검증한다.
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				timestampValidator,
				issuerValidator,
				audienceValidator,
				subjectValidator,
				expirationValidator
		));
		return decoder;
	}

}
