package web.tosunsaeng.identity.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CheckEmailRequest(
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
