package web.tosunsaeng.identity.domain.user.exception;

import org.springframework.http.HttpStatus;

import web.tosunsaeng.identity.global.exception.ErrorCode;

public enum UserErrorStatus implements ErrorCode {

	USER_NOT_FOUND(
			HttpStatus.NOT_FOUND,
			"USER_NOT_FOUND",
			"사용자를 찾을 수 없습니다."
	),
	ACCOUNT_NOT_ACTIVE(
			HttpStatus.FORBIDDEN,
			"ACCOUNT_NOT_ACTIVE",
			"활성 상태가 아닌 계정은 로그인할 수 없습니다."
	);

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	UserErrorStatus(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}

	@Override
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}

	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
