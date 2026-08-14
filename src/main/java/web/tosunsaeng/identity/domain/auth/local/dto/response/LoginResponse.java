package web.tosunsaeng.identity.domain.auth.local.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 성공 시 발급된 인증 토큰")
public record LoginResponse(
		@Schema(description = "RS256 JWT Access Token", accessMode = Schema.AccessMode.READ_ONLY)
		String accessToken,
		@Schema(description = "Opaque Refresh Token", accessMode = Schema.AccessMode.READ_ONLY)
		String refreshToken,
		@Schema(description = "토큰 타입", example = "Bearer", allowableValues = "Bearer")
		String grantType,
		@Schema(description = "Access Token 유효 기간(밀리초)", example = "1800000")
		long accessTokenExpiresIn
) {

	@Override
	public String toString() {
		return "LoginResponse[accessToken=redacted, refreshToken=redacted, grantType="
				+ grantType + ", accessTokenExpiresIn=" + accessTokenExpiresIn + "]";
	}
}
