package web.tosunsaeng.identity.domain.auth.session.domain;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document("logout_all_operations")
@CompoundIndex(name = "uk_logout_user_epoch", def = "{'userId':1,'epoch':1}", unique = true)
@CompoundIndex(name = "ix_logout_due", def = "{'status':1,'nextAttemptAt':1}")
public class LogoutAllOperation {
	public enum Status { PENDING, CLAIMED, REVOKING, VERIFYING, RETRY_WAIT, RECONCILIATION_REQUIRED, COMPLETED, SUPERSEDED_BY_WITHDRAWAL }
	@Id private String logoutId;
	@Version private Long version;
	@Indexed(unique = true, name = "uk_logout_request") private String requestFingerprint;
	private String userId;
	private long epoch;
	private String bindingId;
	private String projectId;
	private String tenantId;
	private String firebaseUid;
	private Instant bindingCreatedAt;
	private Instant remoteCreationTime;
	private Instant dispatchAt;
	private Instant terminalAt;
	private Instant requestedAt;
	private Instant requestExpiresAt;
	private Instant nextAttemptAt;
	private Instant leaseExpiresAt;
	private String leaseOwner;
	private boolean mutationStarted;
	private boolean dispatchAcknowledged;
	private int attemptCount;
	private Status status;
	private String failureCode;
	private Instant observedValidAfter;
	@Indexed(name = "ttl_logout_operations", expireAfter = "0s") private Instant cleanupAt;

	private LogoutAllOperation() { }
	public static LogoutAllOperation create(String fingerprint, String userId, long epoch,
			FirebaseIdentity binding,
			String tenantId, Instant now, Instant requestExpiresAt) {
		LogoutAllOperation op = new LogoutAllOperation();
		op.logoutId = UUID.randomUUID().toString(); op.requestFingerprint = fingerprint;
		op.userId = userId; op.epoch = epoch; op.tenantId = tenantId;
		if (binding != null) {
			op.bindingId = binding.getFirebaseIdentityId(); op.projectId = binding.getFirebaseProjectId();
			op.firebaseUid = binding.getFirebaseUid(); op.bindingCreatedAt = binding.getCreatedAt();
		}
		op.requestedAt = now; op.requestExpiresAt = requestExpiresAt;
		op.nextAttemptAt = now; op.status = Status.PENDING;
		return op;
	}
	public boolean terminal() { return status == Status.COMPLETED || status == Status.SUPERSEDED_BY_WITHDRAWAL; }
	public void claim(String owner, Instant expiresAt) {
		leaseOwner = owner; leaseExpiresAt = expiresAt; status = Status.CLAIMED;
	}
	public void deferQueueCheck(Instant at) { nextAttemptAt = at; }
	public void startMutation(Instant remoteCreatedAt, Instant dispatchAt) {
		if (mutationStarted) throw new IllegalStateException("Mutation already dispatched.");
		remoteCreationTime = remoteCreatedAt; this.dispatchAt = dispatchAt; mutationStarted = true; status = Status.REVOKING;
	}
	public void acknowledge() { dispatchAcknowledged = true; status = Status.VERIFYING; }
	public void observed(Instant validAfter) { observedValidAfter = validAfter; }
	public void unknown(String code) { failureCode = code; status = Status.RECONCILIATION_REQUIRED; nextAttemptAt = null; }
	public void retry(String code, Instant at) {
		failureCode = code;
		attemptCount++;
		nextAttemptAt = at;
		status = Status.RETRY_WAIT;
		leaseOwner = null;
		leaseExpiresAt = null;
	}
	public void complete(Instant terminalAt, Instant cleanupAt) {
		status = Status.COMPLETED;
		this.terminalAt = terminalAt;
		this.cleanupAt = cleanupAt;
		nextAttemptAt = null;
		failureCode = null;
	}
	public void supersede(Instant terminalAt, Instant cleanupAt) {
		if (mutationStarted) throw new IllegalStateException("Unresolved external actor.");
		status = Status.SUPERSEDED_BY_WITHDRAWAL;
		this.terminalAt = terminalAt;
		this.cleanupAt = cleanupAt;
		nextAttemptAt = null;
	}
}
