package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Instant;
import java.util.Objects;

public record IssuedRefreshSession(
		String tokenValue,
		Instant issuedAt,
		Instant expiresAt
) {

	public IssuedRefreshSession {
		Objects.requireNonNull(tokenValue, "tokenValue must not be null");
		Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (!expiresAt.isAfter(issuedAt)) {
			throw new IllegalArgumentException("expiresAt must be after issuedAt");
		}
	}

	@Override
	public String toString() {
		return "IssuedRefreshSession[tokenValue=redacted, issuedAt=" + issuedAt
				+ ", expiresAt=" + expiresAt + "]";
	}
}
