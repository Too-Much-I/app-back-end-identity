package web.tosunsaeng.identity.global.security.jwt;

import java.time.Duration;
import java.util.List;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank String issuer,
		@NotBlank String audience,
		@NotBlank String keyId,
		@NotNull Duration accessTokenTtl,
		@NotBlank String privateKeyLocation,
		@NotBlank String publicKeyLocation,
		@NotEmpty List<@NotBlank String> defaultScopes
) {

	public JwtProperties {
		defaultScopes = defaultScopes == null ? null : List.copyOf(defaultScopes);
	}

	@AssertTrue(message = "access token TTL must be positive")
	public boolean isAccessTokenTtlPositive() {
		return accessTokenTtl == null
				|| (!accessTokenTtl.isZero() && !accessTokenTtl.isNegative());
	}
}
