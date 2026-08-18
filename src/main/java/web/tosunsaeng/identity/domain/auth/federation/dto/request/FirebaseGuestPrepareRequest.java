package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인증된 Guest의 Firebase MEMBER 승격 준비 요청")
public record FirebaseGuestPrepareRequest(
		@Schema(
				description = "Guest가 인증한 Firebase ID Token",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Firebase 인증 정보는 필수입니다.")
		@Size(max = 16_384, message = "Firebase 인증 정보가 너무 깁니다.")
		String firebaseIdToken
) {

	@Override
	public String toString() {
		return "FirebaseGuestPrepareRequest[firebaseIdToken=redacted]";
	}
}
