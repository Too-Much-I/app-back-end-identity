package web.tosunsaeng.identity.domain.auth.common.exception;

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
	ACCOUNT_WITHDRAWN(
			HttpStatus.UNAUTHORIZED,
			"ACCOUNT_WITHDRAWN",
			"탈퇴 처리된 계정입니다."
	),
	INVALID_WITHDRAWAL_CREDENTIALS(
			HttpStatus.UNAUTHORIZED,
			"INVALID_WITHDRAWAL_CREDENTIALS",
			"회원 탈퇴 인증 정보가 올바르지 않습니다."
	),
	WITHDRAWAL_FIREBASE_PROOF_REQUIRED(
			HttpStatus.BAD_REQUEST,
			"WITHDRAWAL_FIREBASE_PROOF_REQUIRED",
			"Firebase 회원 탈퇴에는 최근 Firebase 인증이 필요합니다."
	),
	WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH(
			HttpStatus.BAD_REQUEST,
			"WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH",
			"계정 유형과 회원 탈퇴 인증 방식이 일치하지 않습니다."
	),
	WITHDRAWAL_CLEANUP_PENDING(
			HttpStatus.CONFLICT,
			"WITHDRAWAL_CLEANUP_PENDING",
			"회원 탈퇴 후 계정 정보를 정리하고 있습니다."
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
	FIREBASE_IDENTITY_CONFLICT(
			HttpStatus.CONFLICT,
			"FIREBASE_IDENTITY_CONFLICT",
			"Firebase 계정 연결이 충돌했습니다. 다시 시도해 주세요."
	),
	SOCIAL_IDENTITY_CONFLICT(
			HttpStatus.CONFLICT,
			"SOCIAL_IDENTITY_CONFLICT",
			"소셜 계정 연결이 충돌했습니다. 다시 시도해 주세요."
	),
	FIREBASE_ENROLLMENT_CONFLICT(
			HttpStatus.CONFLICT,
			"FIREBASE_ENROLLMENT_CONFLICT",
			"가입 요청을 완료할 수 없습니다. Firebase 로그인부터 다시 진행해 주세요."
	),
	GUEST_UPGRADE_NOT_ALLOWED(
			HttpStatus.FORBIDDEN,
			"GUEST_UPGRADE_NOT_ALLOWED",
			"현재 계정은 Guest 승격을 진행할 수 없습니다."
	),
	MERGE_REQUIRED(
			HttpStatus.CONFLICT,
			"MERGE_REQUIRED",
			"이미 연결된 MEMBER 계정으로 로그인해야 합니다."
	),
	GUEST_MERGE_NOT_ALLOWED(
			HttpStatus.FORBIDDEN,
			"GUEST_MERGE_NOT_ALLOWED",
			"현재 계정은 Guest 통합을 진행할 수 없습니다."
	),
	GUEST_MERGE_TARGET_CONFLICT(
			HttpStatus.CONFLICT,
			"GUEST_MERGE_TARGET_CONFLICT",
			"통합할 MEMBER 계정을 확정할 수 없습니다."
	),
	GUEST_MERGE_CONFLICT(
			HttpStatus.CONFLICT,
			"GUEST_MERGE_CONFLICT",
			"Guest 통합 처리 중 계정 정보가 변경되었습니다. 다시 시도해 주세요."
	),
	ACCOUNT_MERGED_TOKEN_REJECTED(
			HttpStatus.UNAUTHORIZED,
			"ACCOUNT_MERGED_TOKEN_REJECTED",
			"통합된 Guest 계정의 인증 정보는 사용할 수 없습니다."
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
