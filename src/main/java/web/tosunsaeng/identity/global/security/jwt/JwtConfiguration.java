package web.tosunsaeng.identity.global.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

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
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	public RsaKeyLoader rsaKeyLoader(ApplicationContext applicationContext) {
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
	public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
		return new NimbusJwtEncoder(jwkSource);
	}

	@Bean
	public JwtDecoder jwtDecoder(
			RSAPublicKey publicKey,
			JwtProperties properties,
			Clock clock
	) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.jwtProcessorCustomizer(processor -> processor.setJWSTypeVerifier(
						new DefaultJOSEObjectTypeVerifier<>(JOSEObjectType.JWT)
				))
				.build();

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
		OAuth2TokenValidator<Jwt> keyIdValidator = keyIdValidator(properties.keyId());

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				timestampValidator,
				issuerValidator,
				audienceValidator,
				subjectValidator,
				expirationValidator,
				keyIdValidator
		));
		return decoder;
	}

	private OAuth2TokenValidator<Jwt> keyIdValidator(String expectedKeyId) {
		return jwt -> {
			if (expectedKeyId.equals(jwt.getHeaders().get("kid"))) {
				return OAuth2TokenValidatorResult.success();
			}
			return OAuth2TokenValidatorResult.failure(new OAuth2Error(
					OAuth2ErrorCodes.INVALID_TOKEN,
					"The JWT key identifier is not valid.",
					null
			));
		};
	}
}
