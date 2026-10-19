package web.tosunsaeng.identity.global.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** OpenAPI 전용 실패 모델. 실제 오류 응답은 BaseResponse.failure로 생성한다. */
@Schema(description = "실패 응답. 입력 검증 오류의 result는 상세 배열이며 일반 업무 오류는 null입니다.")
public record ApiErrorResponse(
		@Schema(description = "요청 성공 여부 (실패)", example = "false") boolean isSuccess,
		@Schema(description = "오류 코드", example = "INVALID_REQUEST") String code,
		@Schema(description = "오류 안내", example = "잘못된 요청입니다.") String message,
		@Schema(description = "입력 검증 상세 배열 또는 null. 민감한 rejectedValue는 null입니다.",
				types = {"array", "null"})
		java.util.List<web.tosunsaeng.identity.global.exception.ValidationErrorDetail> result
) { }
