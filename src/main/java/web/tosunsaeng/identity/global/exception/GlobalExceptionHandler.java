package web.tosunsaeng.identity.global.exception;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import web.tosunsaeng.identity.global.response.BaseResponse;
import web.tosunsaeng.identity.global.observability.RequestLogContext;
import web.tosunsaeng.identity.global.observability.SentryExceptionReporter;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final SentryExceptionReporter SENTRY_EXCEPTION_REPORTER =
			new SentryExceptionReporter();

	private static final Set<String> SENSITIVE_FIELD_FRAGMENTS = Set.of(
			"password",
			"passwd",
			"pwd",
			"token",
			"secret",
			"credential",
			"installationid",
			"authorization",
			"apikey",
			"privatekey"
	);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<List<ValidationErrorDetail>>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpServletRequest request
	) {
		List<ValidationErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toValidationErrorDetail)
				.toList();

		return errorResponse(request, CommonErrorStatus.INVALID_REQUEST, errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<BaseResponse<List<ValidationErrorDetail>>> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request
	) {
		List<ValidationErrorDetail> errors = exception.getConstraintViolations().stream()
				.map(this::toValidationErrorDetail)
				.toList();

		return errorResponse(request, CommonErrorStatus.INVALID_REQUEST, errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadable(
			HttpMessageNotReadableException exception,
			HttpServletRequest request
	) {
		return errorResponse(request, CommonErrorStatus.INVALID_REQUEST, null);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<BaseResponse<Void>> handleNoResourceFound(
			NoResourceFoundException exception,
			HttpServletRequest request
	) {
		return errorResponse(request, CommonErrorStatus.NOT_FOUND, null);
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupported(
			HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request
	) {
		return errorResponse(request, CommonErrorStatus.METHOD_NOT_ALLOWED, null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<BaseResponse<Void>> handleHttpMediaTypeNotSupported(
			HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request
	) {
		return errorResponse(request, CommonErrorStatus.UNSUPPORTED_MEDIA_TYPE, null);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Void>> handleBusinessException(
			BusinessException exception,
			HttpServletRequest request
	) {
		return errorResponse(request, exception.getErrorCode(), null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(
			Exception exception,
			HttpServletRequest request
	) {
		RequestLogContext.recordUnexpectedFailure(request, exception);
		SENTRY_EXCEPTION_REPORTER.captureUnexpected(
				exception,
				request,
				CommonErrorStatus.INTERNAL_SERVER_ERROR.getCode()
		);
		return errorResponse(request, CommonErrorStatus.INTERNAL_SERVER_ERROR, null);
	}

	private ValidationErrorDetail toValidationErrorDetail(FieldError error) {
		return new ValidationErrorDetail(
				error.getField(),
				sanitizeRejectedValue(error.getField(), error.getRejectedValue()),
				validationReason(error.getDefaultMessage())
		);
	}

	private ValidationErrorDetail toValidationErrorDetail(ConstraintViolation<?> violation) {
		String field = extractFieldName(violation.getPropertyPath().toString());
		return new ValidationErrorDetail(
				field,
				sanitizeRejectedValue(field, violation.getInvalidValue()),
				validationReason(violation.getMessage())
		);
	}

	private String extractFieldName(String propertyPath) {
		int lastSeparator = propertyPath.lastIndexOf('.');
		return lastSeparator >= 0 ? propertyPath.substring(lastSeparator + 1) : propertyPath;
	}

	private Object sanitizeRejectedValue(String field, Object rejectedValue) {
		String normalizedField = field == null
				? ""
				: field.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);

		// 비밀번호와 토큰 계열의 거부 값은 오류 응답에서 제거한다.
		boolean sensitive = SENSITIVE_FIELD_FRAGMENTS.stream().anyMatch(normalizedField::contains);
		return sensitive ? null : rejectedValue;
	}

	private String validationReason(String reason) {
		return reason == null || reason.isBlank()
				? CommonErrorStatus.INVALID_REQUEST.getMessage()
				: reason;
	}

	private <T> ResponseEntity<BaseResponse<T>> errorResponse(
			HttpServletRequest request,
			ErrorCode errorCode,
			T result
	) {
		RequestLogContext.recordErrorCode(request, errorCode.getCode());
		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(BaseResponse.failure(errorCode, result));
	}
}
