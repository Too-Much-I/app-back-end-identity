package web.tosunsaeng.identity.global.security.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredential;
import web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure.WorkloadIdentityCredentialProvider;
import web.tosunsaeng.identity.global.workload.WorkloadIdentityPurpose;

public final class JwtWorkloadIdentityCredentialProvider
		implements WorkloadIdentityCredentialProvider {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final WorkloadJwtProperties properties;
	private final Clock clock;

	public JwtWorkloadIdentityCredentialProvider(
			JwtEncoder jwtEncoder,
			JwtProperties jwtProperties,
			WorkloadJwtProperties properties,
			Clock clock
	) {
		this.jwtEncoder = Objects.requireNonNull(jwtEncoder);
		this.jwtProperties = Objects.requireNonNull(jwtProperties);
		this.properties = Objects.requireNonNull(properties);
		this.clock = Objects.requireNonNull(clock);
		properties.validate();
	}

	@Override
	public WorkloadIdentityCredential issue(WorkloadIdentityPurpose purpose) {
		String audience = Objects.requireNonNull(
				purpose,
				"purpose must not be null"
		).audience();
		Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = issuedAt.plus(properties.getTtl());
		JwsHeader headers = JwsHeader.with(SignatureAlgorithm.RS256)
				.keyId(jwtProperties.keyId())
				.type("JWT")
				.build();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(properties.getIssuer())
				.audience(List.of(audience))
				.subject(properties.getSubject())
				.issuedAt(issuedAt)
				.notBefore(issuedAt)
				.expiresAt(expiresAt)
				.id(UUID.randomUUID().toString())
				.build();
		Jwt jwt = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));
		return new WorkloadIdentityCredential(jwt.getTokenValue(), issuedAt, expiresAt);
	}
}
