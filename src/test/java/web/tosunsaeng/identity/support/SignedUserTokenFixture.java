package web.tosunsaeng.identity.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

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

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.JwtAccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.JwtConfiguration;
import web.tosunsaeng.identity.global.security.jwt.JwtProperties;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** 서비스 경로 테스트에서 외부 인프라 없이 실제 공통 발급·서명·검증을 사용한다. */
public final class SignedUserTokenFixture {

	private final JwtAccessTokenIssuer issuer;
	private final JwtDecoder decoder;

	public SignedUserTokenFixture(Instant now) {
		KeyPair pair;
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			pair = generator.generateKeyPair();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("RSA unavailable in test runtime", exception);
		}
		JwtProperties properties = new JwtProperties("https://identity.test", "tosunsaeng-learning-core",
				"billing-reader-test", Duration.ofMinutes(30), "unused-private", "unused-public",
				List.of("learning:read", "learning:write"));
		JwtConfiguration configuration = new JwtConfiguration();
		Clock clock = Clock.fixed(now, ZoneOffset.UTC);
		RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();
		issuer = new JwtAccessTokenIssuer(configuration.jwtEncoder(configuration.jwkSource(
				configuration.rsaKey(publicKey, (RSAPrivateKey) pair.getPrivate(), properties))), properties, clock);
		decoder = configuration.jwtDecoder(publicKey, properties, clock);
	}

	public void delegate(AccessTokenIssuer mockIssuer) {
		doAnswer(call -> issuer.issue(call.getArgument(0), call.getArgument(1), call.getArgument(2)))
				.when(mockIssuer).issue(any(), any(), any());
	}

	public void assertClaims(String value, String userId, UserAccountType accountType) {
		Jwt jwt = decoder.decode(value);
		assertThat(jwt.getSubject()).isEqualTo(userId);
		assertThat(jwt.getClaimAsString("account_type")).isEqualTo(accountType.name());
		assertThat(jwt.getAudience()).containsExactly("tosunsaeng-learning-core", "tosunsaeng-billing");
		assertThat(jwt.getClaimAsString("scope")).isEqualTo("billing:read learning:read learning:write");
		assertThat(jwt.getClaims()).containsOnlyKeys("sub", "iss", "aud", "iat", "exp", "jti", "scope", "account_type");
	}
}
