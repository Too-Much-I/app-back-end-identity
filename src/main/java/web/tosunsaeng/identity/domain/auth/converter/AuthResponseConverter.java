package web.tosunsaeng.identity.domain.auth.converter;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Component
public class AuthResponseConverter {

	public LoginResponse toLoginResponse(
			IssuedAccessToken accessToken,
			String refreshTokenValue
	) {
		return new LoginResponse(
				accessToken.tokenValue(),
				refreshTokenValue,
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis()
		);
	}

	public ReissueResponse toReissueResponse(
			IssuedAccessToken accessToken,
			String refreshTokenValue,
			Instant refreshTokenExpiresAt,
			Instant refreshTokenIssuedAt
	) {
		return new ReissueResponse(
				accessToken.tokenValue(),
				refreshTokenValue,
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshTokenIssuedAt, refreshTokenExpiresAt).toMillis()
		);
	}
}
