package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기존 MEMBER의 Identity 인증 토큰 발급 결과")
public record FirebaseAuthenticatedResponse(
		@Schema(description = "교환 결과 종류", allowableValues = "AUTHENTICATED")
		FirebaseExchangeResultType type,
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
) implements FirebaseExchangeResponse {

	public FirebaseAuthenticatedResponse {
		if (type != FirebaseExchangeResultType.AUTHENTICATED) {
			throw new IllegalArgumentException("type must be AUTHENTICATED");
		}
		accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
		refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
		grantType = Objects.requireNonNull(grantType, "grantType must not be null");
		if (accessTokenExpiresIn <= 0 || refreshTokenExpiresIn <= 0) {
			throw new IllegalArgumentException("token lifetimes must be positive");
		}
	}

	public FirebaseAuthenticatedResponse(
			String accessToken,
			String refreshToken,
			String grantType,
			long accessTokenExpiresIn,
			long refreshTokenExpiresIn
	) {
		this(
				FirebaseExchangeResultType.AUTHENTICATED,
				accessToken,
				refreshToken,
				grantType,
				accessTokenExpiresIn,
				refreshTokenExpiresIn
		);
	}

	@Override
	public String toString() {
		return "FirebaseAuthenticatedResponse[type=" + type
				+ ", accessToken=redacted, refreshToken=redacted, grantType=" + grantType
				+ ", accessTokenExpiresIn=" + accessTokenExpiresIn
				+ ", refreshTokenExpiresIn=" + refreshTokenExpiresIn + "]";
	}
}
