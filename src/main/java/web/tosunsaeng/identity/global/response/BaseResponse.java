package web.tosunsaeng.identity.global.response;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.global.exception.ErrorCode;

@Schema(description = "공통 API 응답")
public record BaseResponse<T>(
		@Schema(description = "요청 성공 여부", example = "true")
		boolean isSuccess,
		@Schema(description = "응답 코드", example = "SUCCESS")
		String code,
		@Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
		String message,
		@Schema(description = "응답 데이터")
		T result
) {

	private static final String SUCCESS_CODE = "SUCCESS";
	private static final String SUCCESS_MESSAGE = "요청에 성공했습니다.";

	public static <T> BaseResponse<T> success(T result) {
		return new BaseResponse<>(true, SUCCESS_CODE, SUCCESS_MESSAGE, result);
	}

	public static <T> BaseResponse<T> success(String code, String message, T result) {
		return new BaseResponse<>(true, code, message, result);
	}

	public static BaseResponse<Void> failure(ErrorCode errorCode) {
		return failure(errorCode, null);
	}

	public static <T> BaseResponse<T> failure(ErrorCode errorCode, T result) {
		ErrorCode requiredErrorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
		return new BaseResponse<>(
				false,
				requiredErrorCode.getCode(),
				requiredErrorCode.getMessage(),
				result
		);
	}
}
