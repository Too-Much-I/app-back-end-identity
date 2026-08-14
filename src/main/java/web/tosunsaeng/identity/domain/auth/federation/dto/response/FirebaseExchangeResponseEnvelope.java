package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Firebase 교환 공통 API 응답")
public record FirebaseExchangeResponseEnvelope(
		@Schema(description = "요청 성공 여부", example = "true")
		boolean isSuccess,
		@Schema(description = "응답 코드", example = "SUCCESS")
		String code,
		@Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
		String message,
		@Schema(description = "AUTHENTICATED 또는 ENROLLMENT_REQUIRED 결과")
		FirebaseExchangeResponse result
) {
}
