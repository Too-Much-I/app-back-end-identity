package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** OpenAPI 전용 모델. 런타임 응답은 BaseResponse를 사용한다. */
@Schema(description = "Guest 가입 준비·재개 공통 API 응답")
public record FirebaseGuestPrepareResponseEnvelope(
		@Schema(description = "요청 성공 여부", example = "true")
		boolean isSuccess,
		@Schema(description = "응답 코드", example = "SUCCESS")
		String code,
		@Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
		String message,
		@Schema(description = "ENROLLMENT_REQUIRED는 모든 enrollment 필드를 포함하고, MERGE_REQUIRED는 type만 포함")
		FirebaseGuestPrepareResponse result
) {
}
