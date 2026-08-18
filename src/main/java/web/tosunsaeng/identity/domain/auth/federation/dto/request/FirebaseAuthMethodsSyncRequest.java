package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기존 MEMBER의 Firebase 인증수단 동기화 요청")
public record FirebaseAuthMethodsSyncRequest(
		@Schema(
				description = "인증수단 연결 후 강제 갱신한 Firebase ID Token",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Firebase 인증 정보는 필수입니다.")
		@Size(max = 16_384, message = "Firebase 인증 정보가 너무 깁니다.")
		String firebaseIdToken
) {

	@Override
	public String toString() {
		return "FirebaseAuthMethodsSyncRequest[firebaseIdToken=redacted]";
	}
}
