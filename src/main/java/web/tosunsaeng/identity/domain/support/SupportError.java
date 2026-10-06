package web.tosunsaeng.identity.domain.support;

import org.springframework.http.HttpStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.ErrorCode;

public enum SupportError implements ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 문의 요청입니다."),
    INVALID_SUPPORT_REQUEST_ID(HttpStatus.BAD_REQUEST, "문의 요청 식별자가 올바르지 않습니다."),
    SUPPORT_INQUIRY_REQUEST_CONFLICT(HttpStatus.CONFLICT, "동일 요청 식별자에 다른 내용이 사용되었습니다."),
    SUPPORT_INQUIRY_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "문의 요청 크기를 초과했습니다."),
    SUPPORT_INQUIRY_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "문의 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    SUPPORT_INQUIRY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "문의 접수를 일시적으로 사용할 수 없습니다.");
    private final HttpStatus status;
    private final String message;
    SupportError(HttpStatus status, String message) { this.status = status; this.message = message; }
    public HttpStatus getHttpStatus() { return status; }
    public String getCode() { return name(); }
    public String getMessage() { return message; }
    public BusinessException exception() { return new BusinessException(this); }
}
