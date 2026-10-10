package web.tosunsaeng.identity.domain.auth.common.exception;

/** Closed diagnostic vocabulary; never contains credentials or identifiers. */
public enum SessionRejectionReason {
	UNKNOWN, EPOCH_MISMATCH, LEGACY_AUTH_BOUNDARY, AUTH_EPOCH_MISMATCH,
	FIREBASE_REVOCATION_BOUNDARY, RECOVERY_CANCELLED, SOURCE_REVOKED,
	CHILD_REVOKED, CHILD_EXPIRED
}
