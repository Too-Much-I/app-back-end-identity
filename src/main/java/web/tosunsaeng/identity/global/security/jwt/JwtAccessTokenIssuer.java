package web.tosunsaeng.identity.global.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

@Service
@RequiredArgsConstructor
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private static final String BILLING_AUDIENCE = "tosunsaeng-billing";
	private static final String BILLING_READ_SCOPE = "billing:read";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	@Override
	public IssuedAccessToken issue(String userId, UserAccountType accountType, Set<String> scopes) {
		validateUserId(userId);
		if (accountType == null) {
			throw new IllegalArgumentException("Access Token accountType must not be null.");
		}
		Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		// Scope 순서를 고정해 같은 권한 집합의 Claim 표현을 일관되게 유지한다.
		String scopeClaim = String.join(" ", orderedScopes(scopes));

		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
				.keyId(properties.keyId())
				.type("JWT")
				.build();
		// 검증된 실제 사용자 UUID를 JWT subject로 사용한다.
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userId)
				.issuer(properties.issuer())
				.audience(userAudiences())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.claim("scope", scopeClaim)
				.claim("account_type", accountType.name())
				.build();

		Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));
		return new IssuedAccessToken(
				jwt.getTokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				issuedAt,
				expiresAt,
				properties.accessTokenTtl().getSeconds()
		);
	}

	private void validateUserId(String userId) {
		try {
			UUID uuid = UUID.fromString(userId);
			if (!uuid.toString().equalsIgnoreCase(userId)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException("Access Token userId must be a UUID.");
		}
	}

	private List<String> userAudiences() {
		return List.copyOf(new LinkedHashSet<>(List.of(properties.audience(), BILLING_AUDIENCE)));
	}

	private Collection<String> orderedScopes(Set<String> scopes) {
		Collection<String> selectedScopes = scopes == null || scopes.isEmpty()
				? properties.defaultScopes()
				: scopes;
		TreeSet<String> orderedScopes = new TreeSet<>();
		for (String scope : selectedScopes) {
			if (scope == null || scope.isBlank() || scope.chars().anyMatch(Character::isWhitespace)) {
				throw new IllegalArgumentException("Access Token scopes must be non-blank tokens.");
			}
			orderedScopes.add(scope);
		}
		// 사용자 조회 권한만 추가한다. workload 발급과 기존 scope 선택 규칙은 변경하지 않는다.
		orderedScopes.add(BILLING_READ_SCOPE);
		return orderedScopes;
	}
}
