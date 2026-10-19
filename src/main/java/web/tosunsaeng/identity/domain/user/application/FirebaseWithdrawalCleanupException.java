package web.tosunsaeng.identity.domain.user.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

public final class FirebaseWithdrawalCleanupException extends RuntimeException {

	private final WithdrawalCleanupFailureCode failureCode;

	public FirebaseWithdrawalCleanupException(WithdrawalCleanupFailureCode failureCode) {
		super("Firebase withdrawal cleanup operation failed.");
		this.failureCode = Objects.requireNonNull(failureCode, "failureCode must not be null");
	}

	public WithdrawalCleanupFailureCode failureCode() {
		return failureCode;
	}
}
