package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Instant;

/** Revoke-only boundary: no disable, delete or provider unlink capability. */
public interface FirebaseSessionRevocationPort {
	record Target(String projectId, String tenantId, String uid) {
		@Override public String toString() { return "FirebaseSessionTarget[redacted]"; }
	}
	record Snapshot(Instant createdAt, Instant validAfter, boolean disabled) { }
	Snapshot inspect(Target target);
	void revoke(Target target, Instant dispatchAt);

	final class Failure extends RuntimeException {
		public enum Kind { READ_TRANSIENT, TARGET_INVALID, CONFIGURATION, RESULT_UNKNOWN }
		private final Kind kind;
		public Failure(Kind kind) { super("Firebase session operation failed."); this.kind = kind; }
		public Kind kind() { return kind; }
	}
}
