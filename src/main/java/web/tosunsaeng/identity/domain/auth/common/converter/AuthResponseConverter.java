package web.tosunsaeng.identity.domain.auth.common.converter;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.auth.registration.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.local.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Component
public class AuthResponseConverter {

	public GuestAuthResponse toGuestAuthResponse(
			IssuedAccessToken accessToken,
			String refreshTokenValue,
			Instant refreshTokenIssuedAt,
			Instant refreshTokenExpiresAt
	) {
		return new GuestAuthResponse(
				accessToken.tokenValue(),
				refreshTokenValue,
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshTokenIssuedAt, refreshTokenExpiresAt).toMillis()
		);
	}

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
