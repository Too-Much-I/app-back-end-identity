package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Firebase 인증 정보를 Identity 인증 결과로 교환하는 요청")
public record FirebaseExchangeRequest(
		@Schema(
				description = "Firebase ID Token",
				accessMode = Schema.AccessMode.WRITE_ONLY,
				requiredMode = Schema.RequiredMode.REQUIRED
		)
		String firebaseIdToken
) {

	@Override
	public String toString() {
		return "FirebaseExchangeRequest[firebaseIdToken=redacted]";
	}
}
