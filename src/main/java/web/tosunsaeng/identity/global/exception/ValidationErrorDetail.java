package web.tosunsaeng.identity.global.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입력값 검증 오류 상세")
public record ValidationErrorDetail(
		@Schema(description = "오류가 발생한 필드", example = "email")
		String field,
		@Schema(description = "거부된 값. 민감 필드는 null로 마스킹됩니다.", nullable = true)
		Object rejectedValue,
		@Schema(description = "검증 실패 사유")
		String reason
) {
}
