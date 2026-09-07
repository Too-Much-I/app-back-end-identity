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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.nimbusds.jose.jwk.RSAKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

class JwtAccessTokenIssuerTests {

	private static final Instant NOW = Instant.parse("2026-07-24T01:02:03Z");
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final String ISSUER = "https://identity.test";
	private static final String AUDIENCE = "tosunsaeng-learning-core";
	private static final String KEY_ID = "identity-test-rsa-key";
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String OTHER_GUEST_USER_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";

	private RSAPublicKey publicKey;
	private JwtAccessTokenIssuer accessTokenIssuer;

	@BeforeEach
	void setUp() {
		KeyPair keyPair = generateRsaKeyPair();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
		publicKey = (RSAPublicKey) keyPair.getPublic();
		JwtProperties properties = properties();
		JwtConfiguration configuration = new JwtConfiguration();
		RSAKey rsaKey = configuration.rsaKey(publicKey, privateKey, properties);
		accessTokenIssuer = new JwtAccessTokenIssuer(
				configuration.jwtEncoder(configuration.jwkSource(rsaKey)),
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void issuesRs256TokenWithRequiredHeadersClaimsAndMetadata() {
		IssuedAccessToken issuedToken = accessTokenIssuer.issue(
				USER_ID,
				UserAccountType.MEMBER,
				Set.of("learning:write", "learning:read")
		);

		Jwt jwt = decodeWith(publicKey, issuedToken.tokenValue());

		assertThat(jwt.getHeaders())
				.containsEntry("alg", "RS256")
				.containsEntry("kid", KEY_ID)
				.containsEntry("typ", "JWT");
		assertThat(jwt.getSubject()).isEqualTo(USER_ID);
		assertThat(jwt.getIssuer().toString()).isEqualTo(ISSUER);
		assertThat(jwt.getAudience()).containsExactly(AUDIENCE);
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(ACCESS_TOKEN_TTL));
		assertThat(UUID.fromString(jwt.getId()).toString()).isEqualTo(jwt.getId());
		assertThat(jwt.getClaimAsString("scope"))
				.isEqualTo("learning:read learning:write");
		assertThat(jwt.getClaims()).containsEntry("account_type", "MEMBER");

		assertThat(issuedToken.tokenType()).isEqualTo("Bearer");
		assertThat(issuedToken.issuedAt()).isEqualTo(NOW);
		assertThat(issuedToken.expiresAt()).isEqualTo(NOW.plus(ACCESS_TOKEN_TTL));
		assertThat(issuedToken.expiresInSeconds()).isEqualTo(ACCESS_TOKEN_TTL.getSeconds());
	}

	@ParameterizedTest
	@EnumSource(UserAccountType.class)
	void issuesExactStringAccountTypeForEveryAllowedType(UserAccountType accountType) {
		Jwt jwt = decodeWith(publicKey,
				accessTokenIssuer.issue(USER_ID, accountType, Set.of()).tokenValue());

		assertThat(jwt.getClaims()).containsEntry("account_type", accountType.name());
		assertThat(jwt.getClaims().get("account_type")).isInstanceOf(String.class);
		assertThat(jwt.getClaims()).containsOnlyKeys(
				"sub", "iss", "aud", "iat", "exp", "jti", "scope", "account_type");
	}

	@Test
	void rejectsMissingAccountTypeInsteadOfDefaultingToMember() {
		assertThatThrownBy(() -> accessTokenIssuer.issue(USER_ID, null, Set.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Access Token accountType must not be null.");
	}

	@Test
	void usesDefaultScopesWhenRequestedScopesAreEmptyOrNull() {
		IssuedAccessToken emptyScopesToken = accessTokenIssuer.issue(USER_ID, UserAccountType.MEMBER, Set.of());
		IssuedAccessToken nullScopesToken = accessTokenIssuer.issue(USER_ID, UserAccountType.MEMBER, null);

		assertThat(decodeWith(publicKey, emptyScopesToken.tokenValue()).getClaimAsString("scope"))
				.isEqualTo("learning:read learning:write");
		assertThat(decodeWith(publicKey, nullScopesToken.tokenValue()).getClaimAsString("scope"))
				.isEqualTo("learning:read learning:write");
	}

	@Test
	void rejectsInvalidUserId() {
		assertThatThrownBy(() -> accessTokenIssuer.issue("not-a-uuid", UserAccountType.MEMBER, Set.of("learning:read")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Access Token userId must be a UUID.");
	}

	@Test
	void publicKeyVerifiesSignatureButDifferentPublicKeyDoesNot() {
		IssuedAccessToken issuedToken = accessTokenIssuer.issue(USER_ID, UserAccountType.MEMBER, Set.of("learning:read"));

		assertThat(decodeWith(publicKey, issuedToken.tokenValue()).getSubject()).isEqualTo(USER_ID);
		RSAPublicKey differentPublicKey = (RSAPublicKey) generateRsaKeyPair().getPublic();
		assertThatThrownBy(() -> decodeWith(differentPublicKey, issuedToken.tokenValue()))
				.isInstanceOf(JwtException.class);
	}

	@Test
	void tokenContainsNoCredentialsPersonalDataOrPrivateKeyParameters() {
		IssuedAccessToken issuedToken = accessTokenIssuer.issue(USER_ID, UserAccountType.MEMBER, Set.of("learning:read"));
		Jwt jwt = decodeWith(publicKey, issuedToken.tokenValue());

		assertThat(jwt.getClaims()).doesNotContainKeys(
				"email",
				"nickname",
				"password",
				"passwordHash",
				"rawPassword",
				"installationId",
				"installationIdHash",
				"guestInstallationIdHash",
				"refreshToken",
				"d",
				"p",
				"q",
				"dp",
				"dq",
				"qi",
				"oth"
		);
		assertThat(jwt.getHeaders()).doesNotContainKeys(
				"jwk",
				"d",
				"p",
				"q",
				"dp",
				"dq",
				"qi",
				"oth"
		);
		assertThat(issuedToken.toString())
				.contains("tokenValue=redacted")
				.doesNotContain(issuedToken.tokenValue());
	}

	@Test
	void guestTokensKeepExistingRs256ContractAndSeparateOwnershipBySubject() {
		Jwt firstGuestToken = decodeWith(
				publicKey,
				accessTokenIssuer.issue(USER_ID, UserAccountType.GUEST, Set.of()).tokenValue()
		);
		Jwt secondGuestToken = decodeWith(
				publicKey,
				accessTokenIssuer.issue(OTHER_GUEST_USER_ID, UserAccountType.GUEST, Set.of()).tokenValue()
		);

		assertThat(firstGuestToken.getHeaders())
				.containsEntry("alg", "RS256")
				.containsEntry("kid", KEY_ID);
		assertThat(firstGuestToken.getIssuer().toString()).isEqualTo(ISSUER);
		assertThat(firstGuestToken.getAudience()).containsExactly(AUDIENCE);
		assertThat(firstGuestToken.getSubject()).isEqualTo(USER_ID);
		assertThat(secondGuestToken.getSubject()).isEqualTo(OTHER_GUEST_USER_ID);
		assertThat(firstGuestToken.getClaims()).containsEntry("account_type", "GUEST");
		assertThat(secondGuestToken.getClaims()).containsEntry("account_type", "GUEST");
		assertThat(firstGuestToken.getSubject()).isNotEqualTo(secondGuestToken.getSubject());
		assertThat(firstGuestToken.getClaims()).doesNotContainKeys(
				"installationId",
				"installationIdHash",
				"guestInstallationIdHash",
				"refreshToken",
				"tokenHash"
		);
	}

	private JwtProperties properties() {
		return new JwtProperties(
				ISSUER,
				AUDIENCE,
				KEY_ID,
				ACCESS_TOKEN_TTL,
				"file:not-used-private.pem",
				"file:not-used-public.pem",
				List.of("learning:read", "learning:write")
		);
	}

	private Jwt decodeWith(RSAPublicKey verificationKey, String tokenValue) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(verificationKey)
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
		return decoder.decode(tokenValue);
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
