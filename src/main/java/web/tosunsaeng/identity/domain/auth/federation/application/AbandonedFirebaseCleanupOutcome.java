package web.tosunsaeng.identity.domain.auth.federation.application;

public enum AbandonedFirebaseCleanupOutcome {
	NONE,
	CLEANED,
	RETRY_SCHEDULED,
	RECONCILIATION_REQUIRED,
	LEASE_LOST
}
