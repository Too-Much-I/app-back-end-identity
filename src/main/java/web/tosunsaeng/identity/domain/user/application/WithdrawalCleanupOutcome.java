package web.tosunsaeng.identity.domain.user.application;

public enum WithdrawalCleanupOutcome {
	NONE,
	LOCAL_TARGET_SKIPPED,
	EXTERNAL_DELETED,
	HANDED_OFF,
	RETRY_SCHEDULED,
	RECONCILIATION_REQUIRED,
	LEASE_LOST
}
