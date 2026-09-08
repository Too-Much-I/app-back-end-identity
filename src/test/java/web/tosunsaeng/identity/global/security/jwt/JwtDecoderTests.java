package web.tosunsaeng.identity.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

class JwtDecoderTests {

	private static final Instant NOW = Instant.parse("2026-07-27T02:03:04Z");
	private static final String ISSUER = "https://identity.test";
	private static final String AUDIENCE = "tosunsaeng-learning-core";
	private static final String KEY_ID = "identity-test-rsa-key";
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";

	private JwtConfiguration configuration;
	private JwtProperties properties;
	private KeyPair keyPair;
	private JwtDecoder decoder;
	private JwtEncoder encoder;

	@BeforeEach
	void setUp() {
		configuration = new JwtConfiguration();
		properties = new JwtProperties(
				ISSUER,
				AUDIENCE,
				KEY_ID,
				Duration.ofMinutes(30),
				"file:not-used-private.pem",
				"file:not-used-public.pem",
				List.of("learning:read", "learning:write")
		);
		keyPair = generateRsaKeyPair();
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
		encoder = encoder(keyPair, KEY_ID);
		decoder = configuration.jwtDecoder(
				publicKey,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void decodesValidRs256TokenWithConfiguredIssuerAudienceAndPublicKey() {
		Jwt jwt = decoder.decode(token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID));

		assertThat(jwt.getSubject()).isEqualTo(USER_ID);
		assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
		assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
		assertThat(jwt.getHeaders())
				.containsEntry("alg", "RS256")
				.containsEntry("typ", "JWT")
				.containsEntry("kid", KEY_ID);
	}

	@Test
	void rejectsTokenSignedWithDifferentPrivateKey() {
		JwtEncoder differentEncoder = encoder(generateRsaKeyPair(), KEY_ID);
		String token = token(differentEncoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(Exception.class);
	}

	@ParameterizedTest
	@EnumSource(UserAccountType.class)
	void newUserTokenSatisfiesBothAudienceBoundariesAndBillingRead(UserAccountType accountType) {
		JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(encoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
		String value = issuer.issue(USER_ID, accountType, Set.of()).tokenValue();
		Jwt jwt = decoder.decode(value);
		assertThat(jwt.getAudience()).containsExactly(AUDIENCE, "tosunsaeng-billing");
		assertThat(new JwtAudienceValidator(AUDIENCE).validate(jwt).hasErrors()).isFalse();
		assertThat(new JwtAudienceValidator("tosunsaeng-billing").validate(jwt).hasErrors()).isFalse();
		assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt))
				.extracting(Object::toString)
				.containsExactlyInAnyOrder("SCOPE_billing:read", "SCOPE_learning:read", "SCOPE_learning:write");
	}

	@Test
	void legacyUserTokenRemainsValidForIdentityButLacksBillingAudienceAndPermission() {
		Jwt legacy = decoder.decode(token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID));
		assertThat(legacy.getClaims()).doesNotContainKey("account_type");
		assertThat(new JwtAudienceValidator("tosunsaeng-billing").validate(legacy).hasErrors()).isTrue();
		assertThat(new JwtGrantedAuthoritiesConverter().convert(legacy))
				.extracting(Object::toString).doesNotContain("SCOPE_billing:read");
	}

	@Test
	void billingAudienceDoesNotImplyReadPermissionOrReplaceIdentityAudience() {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER).subject(USER_ID).audience(List.of(AUDIENCE, "tosunsaeng-billing"))
				.issuedAt(NOW).expiresAt(NOW.plusSeconds(60)).id("billing-scope-boundary")
				.claim("scope", "learning:read").build();
		String value = encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").keyId(KEY_ID).build(), claims)).getTokenValue();
		Jwt jwt = decoder.decode(value);
		assertThat(new JwtAudienceValidator("tosunsaeng-billing").validate(jwt).hasErrors()).isFalse();
		assertThat(new JwtGrantedAuthoritiesConverter().convert(jwt))
				.extracting(Object::toString).doesNotContain("SCOPE_billing:read");
		String billingOnly = token(encoder, ISSUER, "tosunsaeng-billing", USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);
		assertThatThrownBy(() -> decoder.decode(billingOnly))
				.isInstanceOf(org.springframework.security.oauth2.jwt.JwtException.class);
	}

	@Test
	void rejectsExpiredAndNotYetValidTokensUsingInjectedClock() {
		String expired = token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.minusSeconds(1), null, "JWT", KEY_ID);
		String notYetValid = token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), NOW.plusSeconds(1), "JWT", KEY_ID);

		assertThatThrownBy(() -> decoder.decode(expired)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(notYetValid)).isInstanceOf(Exception.class);
	}

	@Test
	void rejectsWrongIssuerAndAudience() {
		String wrongIssuer = token(encoder, "https://other-issuer.test", AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);
		String wrongAudience = token(encoder, ISSUER, "other-audience", USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);
		String missingAudience = token(encoder, ISSUER, null, USER_ID,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);

		assertThatThrownBy(() -> decoder.decode(wrongIssuer)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(wrongAudience)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(missingAudience)).isInstanceOf(Exception.class);
	}

	@Test
	void rejectsMissingSubjectAndExpirationButLeavesUuidFormatToCurrentUserProvider() {
		String missingSubject = token(encoder, ISSUER, AUDIENCE, null,
				NOW.plusSeconds(60), null, "JWT", KEY_ID);
		String missingExpiration = token(encoder, ISSUER, AUDIENCE, USER_ID,
				null, null, "JWT", KEY_ID);
		String nonUuidSubject = token(encoder, ISSUER, AUDIENCE, "not-a-uuid",
				NOW.plusSeconds(60), null, "JWT", KEY_ID);

		assertThatThrownBy(() -> decoder.decode(missingSubject)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(missingExpiration)).isInstanceOf(Exception.class);
		assertThat(decoder.decode(nonUuidSubject).getSubject()).isEqualTo("not-a-uuid");
	}

	@Test
	void requiresJwtTypeAndConfiguredKeyIdentifier() throws JOSEException {
		String wrongType = token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "at+jwt", KEY_ID);
		String missingType = token(encoder, ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, null, KEY_ID);
		String wrongKeyId = token(encoder(keyPair, "other-key-id"), ISSUER, AUDIENCE, USER_ID,
				NOW.plusSeconds(60), null, "JWT", "other-key-id");
		String missingKeyId = rawTokenWithoutKeyId();

		assertThatThrownBy(() -> decoder.decode(wrongType)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(missingType)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(wrongKeyId)).isInstanceOf(Exception.class);
		assertThatThrownBy(() -> decoder.decode(missingKeyId)).isInstanceOf(Exception.class);
	}

	private String rawTokenWithoutKeyId() throws JOSEException {
		JWSHeader headers = new JWSHeader.Builder(JWSAlgorithm.RS256)
				.type(JOSEObjectType.JWT)
				.build();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(ISSUER)
				.audience(AUDIENCE)
				.subject(USER_ID)
				.issueTime(Date.from(NOW.minusSeconds(120)))
				.expirationTime(Date.from(NOW.plusSeconds(60)))
				.jwtID("decoder-test-jti")
				.claim("scope", "learning:read learning:write")
				.build();
		SignedJWT signedJwt = new SignedJWT(headers, claims);
		signedJwt.sign(new RSASSASigner((RSAPrivateKey) keyPair.getPrivate()));
		return signedJwt.serialize();
	}

	private JwtEncoder encoder(KeyPair keyPair, String keyId) {
		RSAKey rsaKey = configuration.rsaKey(
				(RSAPublicKey) keyPair.getPublic(),
				(RSAPrivateKey) keyPair.getPrivate(),
				properties(keyId)
		);
		return configuration.jwtEncoder(configuration.jwkSource(rsaKey));
	}

	private JwtProperties properties(String keyId) {
		return new JwtProperties(
				ISSUER,
				AUDIENCE,
				keyId,
				Duration.ofMinutes(30),
				"file:not-used-private.pem",
				"file:not-used-public.pem",
				List.of("learning:read", "learning:write")
		);
	}

	private String token(
			JwtEncoder selectedEncoder,
			String issuer,
			String audience,
			String subject,
			Instant expiresAt,
			Instant notBefore,
			String type,
			String keyId
	) {
		JwsHeader.Builder headers = JwsHeader.with(SignatureAlgorithm.RS256);
		if (type != null) {
			headers.type(type);
		}
		if (keyId != null) {
			headers.keyId(keyId);
		}

		JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
				.issuedAt(NOW.minusSeconds(120))
				.id("decoder-test-jti")
				.claim("scope", "learning:read learning:write");
		if (issuer != null) {
			claims.issuer(issuer);
		}
		if (audience != null) {
			claims.audience(List.of(audience));
		}
		if (subject != null) {
			claims.subject(subject);
		}
		if (expiresAt != null) {
			claims.expiresAt(expiresAt);
		}
		if (notBefore != null) {
			claims.notBefore(notBefore);
		}

		return selectedEncoder.encode(JwtEncoderParameters.from(headers.build(), claims.build()))
				.getTokenValue();
	}

	private KeyPair generateRsaKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA is not available in the test runtime.");
		}
	}
}
