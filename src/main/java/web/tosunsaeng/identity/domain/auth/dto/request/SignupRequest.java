package web.tosunsaeng.identity.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "일반 이메일 회원가입 요청")
public record SignupRequest(
		@Schema(description = "가입 이메일", example = "user@example.com")
		@NotBlank(message = "이메일은 필수입니다.")
		@Email(message = "올바른 이메일 형식이어야 합니다.")
		String email,

		@Schema(
				description = "8자 이상 64자 이하의 계정 비밀번호",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "비밀번호는 필수입니다.")
		@Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
		String password,

		@Schema(description = "사용자 닉네임", example = "토스마스터")
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
		String nickname,

		@Schema(description = "음성 데이터 수집·이용 동의 여부", example = "true")
		@NotNull(message = "음성 데이터 수집·이용 동의 여부는 필수입니다.")
		Boolean isAudioConsent
) {

	public SignupRequest {
		email = trimNullable(email);
		nickname = trimNullable(nickname);
	}

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}

	@Override
	public String toString() {
		return "SignupRequest[redacted]";
	}
}
