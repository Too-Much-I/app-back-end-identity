package web.tosunsaeng.identity.domain.auth.session.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Access Token과 Refresh Token 재발급 요청")
public record ReissueRequest(
		@Schema(
				description = "Rotation할 Opaque Refresh Token",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Refresh Token은 필수입니다.")
		@Size(max = 512, message = "Refresh Token은 512자 이하여야 합니다.")
		String refreshToken
) {

	@Override
	public String toString() {
		return "ReissueRequest[refreshToken=redacted]";
	}
}
