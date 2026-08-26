package web.tosunsaeng.identity.domain.user.domain.enums;

public enum WithdrawalCleanupFailureCode {
	NOT_FOUND(false),
	RATE_LIMITED(true),
	TIMEOUT(true),
	UNAVAILABLE(true),
	DELETE_NOT_CONFIRMED(true),
	RESULT_UNKNOWN(true),
	PROJECT_MISMATCH(false),
	TARGET_OWNERSHIP_MISMATCH(false),
	PERMISSION_DENIED(false),
	CONFIGURATION_ERROR(false),
	PROVIDER_OBLIGATION_REQUIRED(false),
	IDENTITY_RELEASE_PRECONDITION_FAILED(false),
	IDENTITY_RELEASE_OWNERSHIP_MISMATCH(false),
	IDENTITY_RELEASE_PARTIAL_STATE(false),
	INVARIANT_VIOLATION(false);

	private final boolean retryable;

	WithdrawalCleanupFailureCode(boolean retryable) {
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
