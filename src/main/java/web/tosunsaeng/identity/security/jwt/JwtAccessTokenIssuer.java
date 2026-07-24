package web.tosunsaeng.identity.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

@Service
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public JwtAccessTokenIssuer(
			JwtEncoder jwtEncoder,
			JwtProperties properties,
			Clock clock
	) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	@Override
	public IssuedAccessToken issue(String userId, Set<String> scopes) {
		validateUserId(userId);
		Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
		String scopeClaim = String.join(" ", orderedScopes(scopes));

		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
				.keyId(properties.keyId())
				.type("JWT")
				.build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userId)
				.issuer(properties.issuer())
				.audience(List.of(properties.audience()))
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.claim("scope", scopeClaim)
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
		return orderedScopes;
	}
}
