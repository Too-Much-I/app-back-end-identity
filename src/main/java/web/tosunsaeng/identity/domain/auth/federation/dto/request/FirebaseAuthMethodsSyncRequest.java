package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기존 MEMBER의 승인된 Firebase 인증수단 확인 요청. 신규 연결은 providers/link/complete를 사용합니다.")
public record FirebaseAuthMethodsSyncRequest(
		@Schema(
				description = "인증수단 연결 후 강제 갱신한 Firebase ID Token",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Firebase 인증 정보는 필수입니다.")
		@Size(max = 16_384, message = "Firebase 인증 정보가 너무 깁니다.")
		String firebaseIdToken,
		@Schema(deprecated = true, description = "폐기된 허가 필드. 값을 보내면 거절됩니다. providers/link/complete로 전환하세요.")
		@Size(max = 36) String linkAttemptId
) {
	public FirebaseAuthMethodsSyncRequest(String firebaseIdToken) { this(firebaseIdToken, null); }

	@Override
	public String toString() {
		return "FirebaseAuthMethodsSyncRequest[firebaseIdToken=redacted]";
	}
}
