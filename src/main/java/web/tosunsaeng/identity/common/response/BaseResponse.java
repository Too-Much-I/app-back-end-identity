package web.tosunsaeng.identity.common.response;

import java.util.Objects;

import web.tosunsaeng.identity.common.exception.ErrorCode;

public record BaseResponse<T>(
		boolean isSuccess,
		String code,
		String message,
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
