package web.tosunsaeng.identity.common.exception;

public record ValidationErrorDetail(
		String field,
		Object rejectedValue,
		String reason
) {
}
