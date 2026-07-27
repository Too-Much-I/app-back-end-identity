package web.tosunsaeng.identity.auth.dto.response;

import java.time.Duration;
import java.time.Instant;

import web.tosunsaeng.identity.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.security.refresh.IssuedRefreshSession;

public record ReissueResponse(
		String accessToken,
		String refreshToken,
		String grantType,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {

	public static ReissueResponse from(
			IssuedAccessToken accessToken,
			IssuedRefreshSession refreshSession,
			Instant refreshTokenIssuedAt
	) {
		return new ReissueResponse(
				accessToken.tokenValue(),
				refreshSession.tokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshTokenIssuedAt, refreshSession.expiresAt()).toMillis()
		);
	}

	@Override
	public String toString() {
		return "ReissueResponse[accessToken=redacted, refreshToken=redacted, grantType="
				+ grantType + ", accessTokenExpiresIn=" + accessTokenExpiresIn
				+ ", refreshTokenExpiresIn=" + refreshTokenExpiresIn + "]";
	}
}
