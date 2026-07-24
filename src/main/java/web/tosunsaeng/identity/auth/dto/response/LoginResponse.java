package web.tosunsaeng.identity.auth.dto.response;

import java.time.Duration;

import web.tosunsaeng.identity.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.security.refresh.IssuedRefreshSession;

public record LoginResponse(
		String accessToken,
		String refreshToken,
		String grantType,
		long accessTokenExpiresIn
) {

	public static LoginResponse from(
			IssuedAccessToken accessToken,
			IssuedRefreshSession refreshSession
	) {
		long accessTokenExpiresIn = Duration.between(
				accessToken.issuedAt(),
				accessToken.expiresAt()
		).toMillis();
		return new LoginResponse(
				accessToken.tokenValue(),
				refreshSession.tokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				accessTokenExpiresIn
		);
	}

	@Override
	public String toString() {
		return "LoginResponse[accessToken=redacted, refreshToken=redacted, grantType="
				+ grantType + ", accessTokenExpiresIn=" + accessTokenExpiresIn + "]";
	}
}
