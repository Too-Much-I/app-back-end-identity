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
	),
	PRIVACY_CONSENT_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"PRIVACY_CONSENT_REQUIRED",
			"개인정보 처리 동의가 필요합니다."
	),
	PRIVACY_CONSENT_VERSION_MISMATCH(
			HttpStatus.BAD_REQUEST,
			"PRIVACY_CONSENT_VERSION_MISMATCH",
			"현재 개인정보 처리 동의 버전과 일치하지 않습니다."
	),
	TERM_CONSENT_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"TERM_CONSENT_REQUIRED",
			"이용약관 동의가 필요합니다."
	),
	TERM_CONSENT_VERSION_MISMATCH(
			HttpStatus.BAD_REQUEST,
			"TERM_CONSENT_VERSION_MISMATCH",
			"현재 이용약관 동의 버전과 일치하지 않습니다."
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
