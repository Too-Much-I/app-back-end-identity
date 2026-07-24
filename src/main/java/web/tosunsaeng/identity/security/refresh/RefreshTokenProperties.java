package web.tosunsaeng.identity.security.refresh;

import java.time.Duration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.refresh-token")
public record RefreshTokenProperties(
		@NotNull Duration ttl,
		@Min(32) int randomBytes
) {

	public RefreshTokenProperties {
		if (ttl == null) {
			throw new IllegalArgumentException("app.refresh-token.ttl must not be null");
		}
		if (ttl.isZero() || ttl.isNegative()) {
			throw new IllegalArgumentException("app.refresh-token.ttl must be positive");
		}
		if (randomBytes < 32) {
			throw new IllegalArgumentException("app.refresh-token.random-bytes must be at least 32");
		}
	}
}
