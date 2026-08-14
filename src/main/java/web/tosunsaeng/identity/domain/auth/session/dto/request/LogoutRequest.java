package web.tosunsaeng.identity.domain.auth.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "단일 로그인 세션 로그아웃 요청")
public record LogoutRequest(
		@Schema(
				description = "폐기할 Opaque Refresh Token",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Refresh Token은 필수입니다.")
		@Size(max = 512, message = "Refresh Token은 512자 이하여야 합니다.")
		String refreshToken
) {

	@Override
	public String toString() {
		return "LogoutRequest[refreshToken=redacted]";
	}
}
