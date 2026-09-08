package web.tosunsaeng.identity.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
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
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

class JwtWorkloadIdentityCredentialProviderTests {

	private static final Instant NOW = Instant.parse("2026-08-28T03:04:05Z");

	@Test
	void issuesApprovedWorkloadProfileWithNbfEqualToIat() throws Exception {
		KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
		JwtProperties jwtProperties = jwtProperties();
		JwtConfiguration configuration = new JwtConfiguration();
		RSAKey rsaKey = configuration.rsaKey(
				(RSAPublicKey) keyPair.getPublic(),
				(RSAPrivateKey) keyPair.getPrivate(),
				jwtProperties
		);
		WorkloadJwtProperties properties = enabledProperties();
		JwtEncoder encoder = configuration.jwtEncoder(configuration.jwkSource(rsaKey));
		JwtAccessTokenIssuer userIssuer = new JwtAccessTokenIssuer(
				encoder, jwtProperties, Clock.fixed(NOW, ZoneOffset.UTC));
		IssuedAccessToken userToken = userIssuer.issue(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86", UserAccountType.MEMBER, Set.of());
		JwtWorkloadIdentityCredentialProvider provider = new JwtWorkloadIdentityCredentialProvider(
				encoder,
				jwtProperties,
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		WorkloadIdentityCredential credential = provider.issue(
				WorkloadIdentityPurpose.USER_WITHDRAWN
		);
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic())
				.signatureAlgorithm(SignatureAlgorithm.RS256)
				.build();
		decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
		Jwt jwt = decoder.decode(credential.tokenValue());

		assertThat(jwt.getHeaders()).containsEntry("alg", "RS256")
				.containsEntry("typ", "JWT")
				.containsEntry("kid", jwtProperties.keyId());
		assertThat(jwt.getIssuer().toString()).isEqualTo("https://identity.test/workload");
		assertThat(jwt.getAudience()).containsExactly("learning-core-user-withdrawn");
		assertThat(jwt.getSubject()).isEqualTo("identity-service");
		assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
		assertThat(jwt.getNotBefore()).isEqualTo(NOW);
		assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(2)));
		assertThat(UUID.fromString(jwt.getId()).toString()).isEqualTo(jwt.getId());
		assertThat(jwt.getClaims()).doesNotContainKeys(
				"account_type", "service", "scope", "userId", "email", "phone", "firebaseUid",
				"providerSubject", "credential");

		Jwt userMerged = decoder.decode(provider.issue(
				WorkloadIdentityPurpose.USER_MERGED).tokenValue());
		Jwt nextUserMerged = decoder.decode(provider.issue(
				WorkloadIdentityPurpose.USER_MERGED).tokenValue());
		assertThat(userMerged.getAudience()).containsExactly("learning-core-user-merged");
		assertThat(userMerged.getClaims()).doesNotContainKeys("account_type", "scope");
		assertThat(nextUserMerged.getClaims()).doesNotContainKeys("account_type", "scope");
		assertThat(nextUserMerged.getAudience()).containsExactly("learning-core-user-merged");
		assertThat(userMerged.getId()).isNotEqualTo(nextUserMerged.getId());
		for (WorkloadIdentityPurpose purpose : WorkloadIdentityPurpose.values()) {
			String workloadValue = provider.issue(purpose).tokenValue();
			assertThatThrownBy(() -> configuration.jwtDecoder(
					(RSAPublicKey) keyPair.getPublic(), jwtProperties, Clock.fixed(NOW, ZoneOffset.UTC)
			).decode(workloadValue)).isInstanceOf(JwtException.class);
			// 소비자 계약 fixture: 같은 서명 키라도 사용자 issuer/audience는 workload가 아니다.
			String workloadAudience = decoder.decode(workloadValue).getAudience().getFirst();
			NimbusJwtDecoder workloadDecoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic())
					.signatureAlgorithm(SignatureAlgorithm.RS256).build();
			workloadDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
					new JwtIssuerValidator(properties.getIssuer()), new JwtAudienceValidator(workloadAudience)));
			assertThatThrownBy(() -> workloadDecoder.decode(userToken.tokenValue())).isInstanceOf(JwtException.class);
		}
		Jwt userAfterWorkload = decoder.decode(userIssuer.issue(
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86", UserAccountType.GUEST, Set.of()).tokenValue());
		assertThat(userAfterWorkload.getAudience()).containsExactly("tosunsaeng-learning-core", "tosunsaeng-billing");
		assertThat(userAfterWorkload.getClaimAsString("scope")).isEqualTo("billing:read learning:read");
		assertThat(jwtProperties.audience()).isEqualTo("tosunsaeng-learning-core");
		assertThat(jwtProperties.defaultScopes()).containsExactly("learning:read");
	}

	@Test
	void exposesOnlyApprovedPurposesAndRejectsInvalidConfiguration() {
		WorkloadJwtProperties invalid = enabledProperties();
		invalid.setIssuer("http://identity.test/workload");
		assertThatThrownBy(invalid::validate).isInstanceOf(IllegalArgumentException.class);

		WorkloadJwtProperties properties = enabledProperties();
		JwtWorkloadIdentityCredentialProvider provider = new JwtWorkloadIdentityCredentialProvider(
				parameters -> { throw new AssertionError("must not encode"); },
				jwtProperties(),
				properties,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		assertThat(WorkloadIdentityPurpose.values()).containsExactly(
				WorkloadIdentityPurpose.USER_WITHDRAWN,
				WorkloadIdentityPurpose.USER_MERGED);
		assertThatThrownBy(() -> provider.issue(null))
				.isInstanceOf(NullPointerException.class);
	}

	private WorkloadJwtProperties enabledProperties() {
		WorkloadJwtProperties properties = new WorkloadJwtProperties();
		properties.setEnabled(true);
		properties.setIssuer("https://identity.test/workload");
		return properties;
	}

	private JwtProperties jwtProperties() {
		return new JwtProperties(
				"https://identity.test",
				"tosunsaeng-learning-core",
				"identity-test-rsa-key",
				Duration.ofMinutes(30),
				"unused-private",
				"unused-public",
				List.of("learning:read")
		);
	}
}
