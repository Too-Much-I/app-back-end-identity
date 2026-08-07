package web.tosunsaeng.identity.domain.user.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;

record WithdrawalTransactionResult(
		WithdrawResponse response,
		int revokedSessionCount
) {

	WithdrawalTransactionResult {
		Objects.requireNonNull(response, "response must not be null");
		if (revokedSessionCount < 0) {
			throw new IllegalArgumentException("revokedSessionCount must not be negative");
		}
	}
}
