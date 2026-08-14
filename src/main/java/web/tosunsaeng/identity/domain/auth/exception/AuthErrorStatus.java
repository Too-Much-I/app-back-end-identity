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
	),
	INVALID_WITHDRAWAL_CREDENTIALS(
			HttpStatus.UNAUTHORIZED,
			"INVALID_WITHDRAWAL_CREDENTIALS",
			"회원 탈퇴 인증 정보가 올바르지 않습니다."
	),
	INVALID_FIREBASE_ID_TOKEN(
			HttpStatus.UNAUTHORIZED,
			"INVALID_FIREBASE_ID_TOKEN",
			"유효하지 않은 Firebase 인증 정보입니다."
	),
	FIREBASE_RECENT_AUTH_REQUIRED(
			HttpStatus.UNAUTHORIZED,
			"FIREBASE_RECENT_AUTH_REQUIRED",
			"최근 Firebase 인증이 필요합니다."
	),
	FIREBASE_ACCOUNT_NOT_ALLOWED(
			HttpStatus.FORBIDDEN,
			"FIREBASE_ACCOUNT_NOT_ALLOWED",
			"사용할 수 없는 Firebase 계정입니다."
	),
	FIREBASE_PROVIDER_NOT_ALLOWED(
			HttpStatus.FORBIDDEN,
			"FIREBASE_PROVIDER_NOT_ALLOWED",
			"허용되지 않은 Firebase 인증 수단입니다."
	),
	FIREBASE_EMAIL_VERIFICATION_REQUIRED(
			HttpStatus.FORBIDDEN,
			"FIREBASE_EMAIL_VERIFICATION_REQUIRED",
			"이메일 인증이 필요합니다."
	),
	FIREBASE_PHONE_VERIFICATION_REQUIRED(
			HttpStatus.FORBIDDEN,
			"FIREBASE_PHONE_VERIFICATION_REQUIRED",
			"전화번호 인증이 필요합니다."
	),
	FIREBASE_RATE_LIMITED(
			HttpStatus.TOO_MANY_REQUESTS,
			"FIREBASE_RATE_LIMITED",
			"Firebase 인증 요청이 일시적으로 제한되었습니다."
	),
	FIREBASE_UNAVAILABLE(
			HttpStatus.SERVICE_UNAVAILABLE,
			"FIREBASE_UNAVAILABLE",
			"Firebase 인증을 일시적으로 사용할 수 없습니다."
	),
	INVALID_PHONE_NUMBER(
			HttpStatus.BAD_REQUEST,
			"INVALID_PHONE_NUMBER",
			"유효하지 않은 전화번호 형식입니다."
	),
	PHONE_ALREADY_LINKED(
			HttpStatus.CONFLICT,
			"PHONE_ALREADY_LINKED",
			"이미 다른 사용자에게 연결된 전화번호입니다."
	),
	PHONE_IDENTITY_CONFLICT(
			HttpStatus.CONFLICT,
			"PHONE_IDENTITY_CONFLICT",
			"전화번호 연결이 충돌했습니다. 다시 시도해 주세요."
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
