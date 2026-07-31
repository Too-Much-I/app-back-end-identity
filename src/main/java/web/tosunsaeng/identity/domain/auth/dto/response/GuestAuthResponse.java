package web.tosunsaeng.identity.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Guest 인증 성공 시 발급된 기존 형식의 인증 토큰")
public record GuestAuthResponse(
		@Schema(description = "RS256 JWT Access Token", accessMode = Schema.AccessMode.READ_ONLY)
		String accessToken,
		@Schema(description = "Opaque Refresh Token", accessMode = Schema.AccessMode.READ_ONLY)
		String refreshToken,
		@Schema(description = "토큰 타입", example = "Bearer", allowableValues = "Bearer")
		String grantType,
		@Schema(description = "Access Token 유효 기간(밀리초)", example = "1800000")
		long accessTokenExpiresIn,
		@Schema(description = "Refresh Token 유효 기간(밀리초)", example = "1209600000")
		long refreshTokenExpiresIn
) {

	@Override
	public String toString() {
		return "GuestAuthResponse[accessToken=redacted, refreshToken=redacted, grantType="
				+ grantType + ", accessTokenExpiresIn=" + accessTokenExpiresIn
				+ ", refreshTokenExpiresIn=" + refreshTokenExpiresIn + "]";
	}
}
