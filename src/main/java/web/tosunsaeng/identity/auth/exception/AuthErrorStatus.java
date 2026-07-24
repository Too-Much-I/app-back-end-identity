package web.tosunsaeng.identity.auth.exception;

import org.springframework.http.HttpStatus;

import web.tosunsaeng.identity.common.exception.ErrorCode;

public enum AuthErrorStatus implements ErrorCode {

	EMAIL_ALREADY_EXISTS(
			HttpStatus.CONFLICT,
			"EMAIL_ALREADY_EXISTS",
			"이미 사용 중인 이메일입니다."
	),
	AUDIO_CONSENT_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"AUDIO_CONSENT_REQUIRED",
			"음성 데이터 수집·이용 동의가 필요합니다."
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
