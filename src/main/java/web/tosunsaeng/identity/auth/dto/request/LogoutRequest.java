package web.tosunsaeng.identity.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
		@NotBlank(message = "Refresh Token은 필수입니다.")
		@Size(max = 512, message = "Refresh Token은 512자 이하여야 합니다.")
		String refreshToken
) {

	@Override
	public String toString() {
		return "LogoutRequest[refreshToken=redacted]";
	}
}
