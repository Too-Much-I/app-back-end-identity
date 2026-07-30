package web.tosunsaeng.identity.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일반 이메일 로그인 요청")
public record LoginRequest(
		@Schema(description = "로그인 이메일", example = "user@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "올바른 이메일 형식이어야 합니다.")
		String email,

		@Schema(
				description = "계정 비밀번호",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
		String password
) {

	public LoginRequest {
		email = email == null ? null : email.trim();
	}

	@Override
	public String toString() {
		return "LoginRequest[redacted]";
	}
}
