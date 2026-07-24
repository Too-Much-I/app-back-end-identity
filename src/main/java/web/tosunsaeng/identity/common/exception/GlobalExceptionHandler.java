package web.tosunsaeng.identity.common.exception;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import web.tosunsaeng.identity.common.response.BaseResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
	private static final Set<String> SENSITIVE_FIELD_FRAGMENTS = Set.of(
			"password",
			"passwd",
			"pwd",
			"token",
			"secret",
			"credential",
			"authorization",
			"apikey",
			"privatekey"
	);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<BaseResponse<List<ValidationErrorDetail>>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception
	) {
		List<ValidationErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
				.map(this::toValidationErrorDetail)
				.toList();

		return errorResponse(CommonErrorStatus.INVALID_REQUEST, errors);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<BaseResponse<List<ValidationErrorDetail>>> handleConstraintViolation(
			ConstraintViolationException exception
	) {
		List<ValidationErrorDetail> errors = exception.getConstraintViolations().stream()
				.map(this::toValidationErrorDetail)
				.toList();

		return errorResponse(CommonErrorStatus.INVALID_REQUEST, errors);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException exception) {
		return errorResponse(exception.getErrorCode(), null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unhandled exception type: {}", exception.getClass().getName());
		return errorResponse(CommonErrorStatus.INTERNAL_SERVER_ERROR, null);
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

		boolean sensitive = SENSITIVE_FIELD_FRAGMENTS.stream().anyMatch(normalizedField::contains);
		return sensitive ? null : rejectedValue;
	}

	private String validationReason(String reason) {
		return reason == null || reason.isBlank()
				? CommonErrorStatus.INVALID_REQUEST.getMessage()
				: reason;
	}

	private <T> ResponseEntity<BaseResponse<T>> errorResponse(ErrorCode errorCode, T result) {
		return ResponseEntity
				.status(errorCode.getHttpStatus())
				.body(BaseResponse.failure(errorCode, result));
	}
}
