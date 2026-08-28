package web.tosunsaeng.identity.domain.auth.domain.enums;

public enum AbandonedFirebaseEnrollmentCleanupStatus {

	RESUMABLE,
	CLEANUP_IN_PROGRESS,
	RETRY_WAIT,
	FINALIZED,
	CLEANED,
	RECONCILIATION_REQUIRED
}
