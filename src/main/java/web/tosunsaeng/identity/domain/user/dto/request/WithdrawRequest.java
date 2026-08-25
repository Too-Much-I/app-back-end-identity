package web.tosunsaeng.identity.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "회원 탈퇴 재인증 요청")
public record WithdrawRequest(
		@Schema(
				description = "현재 사용자가 소유한 Opaque Refresh Token",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Refresh Token은 필수입니다.")
		@Size(max = 512, message = "Refresh Token은 512자 이하여야 합니다.")
		String refreshToken,

		@Schema(
				description = "LOCAL 사용자의 현재 비밀번호. GUEST는 생략합니다.",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY,
				nullable = true
		)
		@Size(max = 64, message = "비밀번호는 64자 이하여야 합니다.")
		String password,

		@Schema(
				description = "Firebase/SNS 회원의 최근 인증 Firebase ID Token",
				accessMode = Schema.AccessMode.WRITE_ONLY,
				nullable = true
		)
		@Size(max = 16384, message = "Firebase 인증 정보가 너무 깁니다.")
		String firebaseIdToken
) {
	public WithdrawRequest(String refreshToken, String password) {
		this(refreshToken, password, null);
	}

	@Override
	public String toString() {
		return "WithdrawRequest[redacted]";
	}
}
