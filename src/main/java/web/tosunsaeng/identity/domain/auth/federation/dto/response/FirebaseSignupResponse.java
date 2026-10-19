package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Firebase 신규 MEMBER 가입 및 Identity Token 발급 결과")
public record FirebaseSignupResponse(
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

	public FirebaseSignupResponse {
		accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
		refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
		grantType = Objects.requireNonNull(grantType, "grantType must not be null");
		if (accessTokenExpiresIn <= 0 || refreshTokenExpiresIn <= 0) {
			throw new IllegalArgumentException("token lifetimes must be positive");
		}
	}

	@Override
	public String toString() {
		return "FirebaseSignupResponse[accessToken=redacted, refreshToken=redacted, "
				+ "grantType=" + grantType + ", accessTokenExpiresIn="
				+ accessTokenExpiresIn + ", refreshTokenExpiresIn="
				+ refreshTokenExpiresIn + "]";
	}
}
