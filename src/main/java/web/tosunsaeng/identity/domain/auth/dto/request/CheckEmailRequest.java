package web.tosunsaeng.identity.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 사용 가능 여부 확인 요청")
public record CheckEmailRequest(
		@Schema(description = "확인할 이메일", example = "user@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "올바른 이메일 형식이어야 합니다.")
		String email
) {

	public CheckEmailRequest {
		email = trimNullable(email);
	}

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}
}
