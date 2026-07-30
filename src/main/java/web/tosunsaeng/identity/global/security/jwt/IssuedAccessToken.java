package web.tosunsaeng.identity.global.security.jwt;

import java.time.Instant;

public record IssuedAccessToken(
		String tokenValue,
		String tokenType,
		Instant issuedAt,
		Instant expiresAt,
		long expiresInSeconds
) {

	public static final String BEARER_TOKEN_TYPE = "Bearer";

	@Override
	public String toString() {
		return "IssuedAccessToken[tokenValue=redacted, tokenType=" + tokenType
				+ ", issuedAt=" + issuedAt
				+ ", expiresAt=" + expiresAt
				+ ", expiresInSeconds=" + expiresInSeconds + "]";
	}
}
