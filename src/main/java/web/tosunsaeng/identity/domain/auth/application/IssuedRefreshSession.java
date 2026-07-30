package web.tosunsaeng.identity.domain.auth.application;

import java.time.Instant;
import java.util.Objects;

public record IssuedRefreshSession(
		String tokenValue,
		Instant expiresAt
) {

	public IssuedRefreshSession {
		Objects.requireNonNull(tokenValue, "tokenValue must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
	}

	@Override
	public String toString() {
		return "IssuedRefreshSession[tokenValue=redacted, expiresAt=" + expiresAt + "]";
	}
}
