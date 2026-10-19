package web.tosunsaeng.identity.domain.auth.accountrecovery;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.global.exception.BusinessException;

/** Only local budgets have a known reset time; upstream throttling must not invent one. */
public final class RecoveryRateLimitException extends BusinessException {

	private final long retryAfterSeconds;

	public RecoveryRateLimitException(long retryAfterSeconds) {
		super(AuthErrorStatus.RECOVERY_RATE_LIMITED);
		if (retryAfterSeconds < 1) throw new IllegalArgumentException("Retry delay must be positive");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() { return retryAfterSeconds; }
}
