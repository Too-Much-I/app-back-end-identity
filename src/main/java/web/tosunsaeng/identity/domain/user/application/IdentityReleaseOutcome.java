package web.tosunsaeng.identity.domain.user.application;

public enum IdentityReleaseOutcome {
	DEPENDENCY_PENDING,
	NONE,
	CLEANED,
	IDEMPOTENT,
	RECONCILIATION_REQUIRED,
	CONCURRENT_CHANGE,
	TRANSIENT_FAILURE
}
