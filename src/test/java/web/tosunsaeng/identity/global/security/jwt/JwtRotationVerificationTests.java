package web.tosunsaeng.identity.global.security.jwt;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.time.*;
import java.util.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import web.tosunsaeng.identity.global.config.SecurityConfig;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

@SpringJUnitConfig({SecurityConfig.class, JwtRotationVerificationTests.Fixture.class, JwtRotationVerificationTests.Probe.class})
@WebAppConfiguration
class JwtRotationVerificationTests {
	static final Instant NOW = Instant.parse("2026-09-22T00:00:00Z");
	static final KeyPair ACTIVE = pair(), OLD = pair(), UNKNOWN = pair();
	static final String USER = "00000000-0000-4000-8000-000000000001";
	static final JwtProperties PROPERTIES = new JwtProperties("https://identity.test", "tosunsaeng-learning-core",
			"active", Duration.ofMinutes(30), "unused-private", "unused-public", List.of("learning:read"));
	@Autowired JwtDecoder decoder;
	@Autowired JwtEncoder encoder;
	@Autowired JwksPublicKeySet keys;
	@Autowired WebApplicationContext context;

	@Configuration @EnableWebMvc
	static class Fixture {
		@Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
		@Bean JwksPublicKeySet keys() {
			var loader = mock(RsaKeyLoader.class);
			when(loader.loadPublicKey("unused-old-public")).thenReturn((RSAPublicKey) OLD.getPublic());
			var rotation = new JwksRotationProperties();
			rotation.setPreviousKeyIds("old"); rotation.setPreviousPublicKeyLocations("unused-old-public");
			return new JwtConfiguration().jwksPublicKeySet(activeKey(), loader, PROPERTIES, rotation);
		}
		@Bean JwtDecoder decoder(JwksPublicKeySet keys) {
			return new JwtConfiguration().jwtDecoder(keys, PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
		}
		@Bean JwtEncoder encoder() {
			var config = new JwtConfiguration();
			return config.jwtEncoder(config.jwkSource(activeKey()));
		}
	}
	@RestController static class Probe {
		@GetMapping("/rotation-probe") String probe() { return "authenticated"; }
	}
	@ParameterizedTest @ValueSource(strings = {"active", "old"})
	void bothTrustedKeysReachProtectedHttpEndpoint(String kid) throws Exception {
		String token = token(kid.equals("old") ? OLD : ACTIVE, kid, "valid");
		assertThat(decoder.decode(token).getClaimAsString("account_type")).isEqualTo("MEMBER");
		MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
				.perform(get("/rotation-probe").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(content().string("authenticated"));
	}
	@ParameterizedTest
	@ValueSource(strings = {"unknown", "missing", "blank", "wrong-signature", "wrong-issuer", "wrong-audience",
			"expired", "future", "wrong-type", "missing-type", "RS512", "HS256", "workload"})
	void invalidTokensRemainRejectedByDecoderAndHttp(String variant) throws Exception {
		String kid = switch (variant) { case "unknown" -> "unknown"; case "missing" -> null; case "blank" -> " "; default -> "old"; };
		String token = token(variant.equals("wrong-signature") ? UNKNOWN : OLD, kid, variant);
		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
		MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build()
				.perform(get("/rotation-probe").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}
	@Test void removingOldKeyRejectsOldTokenAndNewIssuanceUsesOnlyActive() throws Exception {
		var activeOnly = new JwtConfiguration().jwtDecoder(new JwksPublicKeySet(new JWKSet(activeKey())),
				PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC));
		String old = token(OLD, "old", "valid");
		assertThatThrownBy(() -> activeOnly.decode(old)).isInstanceOf(JwtException.class);
		String issued = new JwtAccessTokenIssuer(encoder, PROPERTIES, Clock.fixed(NOW, ZoneOffset.UTC))
				.issue(USER, UserAccountType.MEMBER, Set.of()).tokenValue();
		assertThat(decoder.decode(issued).getHeaders().get("kid")).isEqualTo("active");
		assertThat(activeOnly.decode(issued).getSubject()).isEqualTo(USER);
		JWKSet published = JWKSet.parse(keys.toJsonObject());
		assertThat(published.getKeys()).extracting(JWK::getKeyID).containsExactly("active", "old");
		assertThat(published.getKeys()).noneMatch(JWK::isPrivate);
	}
	private static RSAKey activeKey() {
		return new JwtConfiguration().rsaKey((RSAPublicKey) ACTIVE.getPublic(), (RSAPrivateKey) ACTIVE.getPrivate(), PROPERTIES);
	}
	private static String token(KeyPair pair, String kid, String variant) throws Exception {
		JWSAlgorithm algorithm = variant.equals("HS256") ? JWSAlgorithm.HS256 : variant.equals("RS512") ? JWSAlgorithm.RS512 : JWSAlgorithm.RS256;
		var header = new JWSHeader.Builder(algorithm).keyID(kid);
		if (!variant.equals("missing-type")) header.type(new JOSEObjectType(variant.equals("wrong-type") ? "at+jwt" : "JWT"));
		// Even malicious remote key URLs must never become a source of trust.
		header.jwkURL(java.net.URI.create("https://not-a-key-source.invalid/jwks"));
		var claims = new JWTClaimsSet.Builder().subject(USER).claim("account_type", "MEMBER")
				.issuer(variant.equals("wrong-issuer") || variant.equals("workload") ? "https://identity.test/workload" : PROPERTIES.issuer())
				.audience(variant.equals("wrong-audience") || variant.equals("workload") ? "learning-core-user-merged" : PROPERTIES.audience())
				.issueTime(Date.from(NOW.minusSeconds(60))).notBeforeTime(Date.from(variant.equals("future") ? NOW.plusSeconds(1) : NOW.minusSeconds(60)))
				.expirationTime(Date.from(variant.equals("expired") ? NOW.minusSeconds(1) : NOW.plusSeconds(60)))
				.jwtID(UUID.randomUUID().toString()).build();
		var signed = new SignedJWT(header.build(), claims);
		signed.sign(algorithm.equals(JWSAlgorithm.HS256) ? new MACSigner(new byte[32]) : new RSASSASigner((RSAPrivateKey) pair.getPrivate()));
		return signed.serialize();
	}
	private static KeyPair pair() {
		try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
		catch (GeneralSecurityException error) { throw new IllegalStateException("RSA unavailable in test runtime"); }
	}
}
