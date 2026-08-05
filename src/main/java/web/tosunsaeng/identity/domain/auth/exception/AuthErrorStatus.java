package web.tosunsaeng.identity.domain.auth.exception;

import org.springframework.http.HttpStatus;

import web.tosunsaeng.identity.global.exception.ErrorCode;

public enum AuthErrorStatus implements ErrorCode {

	EMAIL_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"EMAIL_ALREADY_EXISTS",
			"이미 사용 중인 이메일입니다."
	),
	GUEST_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"GUEST_ALREADY_EXISTS",
			"이미 생성된 Guest 사용자입니다."
	),
	INVALID_CREDENTIALS(
			HttpStatus.UNAUTHORIZED,
			"INVALID_CREDENTIALS",
			"이메일 또는 비밀번호가 올바르지 않습니다."
	),
	INVALID_REFRESH_TOKEN(
			HttpStatus.UNAUTHORIZED,
			"INVALID_REFRESH_TOKEN",
			"유효하지 않은 Refresh Token"
	),
	REFRESH_TOKEN_EXPIRED(
			HttpStatus.UNAUTHORIZED,
			"REFRESH_TOKEN_EXPIRED",
			"만료된 Refresh Token"
	),
	REFRESH_TOKEN_REUSE_DETECTED(
			HttpStatus.UNAUTHORIZED,
			"REFRESH_TOKEN_REUSE_DETECTED",
			"이미 사용된 Refresh Token"
	);

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	AuthErrorStatus(HttpStatus httpStatus, String code, String message) {
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
