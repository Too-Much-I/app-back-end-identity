package web.tosunsaeng.identity.domain.auth.domain.enums;

public enum AbandonedFirebaseEnrollmentFailureCode {

	OWNER_PRESENT(false),
	BOUND_GUEST_INVALID(false),
	ATTEMPT_STATE_CONFLICT(false),
	INVARIANT_VIOLATION(false),
	PROJECT_MISMATCH(false),
	NOT_FOUND(false),
	RATE_LIMITED(true),
	TIMEOUT(true),
	UNAVAILABLE(true),
	PERMISSION_DENIED(false),
	CONFIGURATION_ERROR(false),
	PROVIDER_OBLIGATION_REQUIRED(false),
	RESULT_UNKNOWN(true),
	DELETE_NOT_CONFIRMED(true);

	private final boolean retryable;

	AbandonedFirebaseEnrollmentFailureCode(boolean retryable) {
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
