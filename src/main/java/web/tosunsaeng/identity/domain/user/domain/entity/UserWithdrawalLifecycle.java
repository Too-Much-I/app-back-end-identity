package web.tosunsaeng.identity.domain.user.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;

@Document(collection = "user_withdrawal_lifecycles")
@CompoundIndex(
		name = "ix_withdrawal_lifecycle_worker_claim",
		def = "{ 'status': 1, 'nextAttemptAt': 1, 'leaseUntil': 1 }"
)
public class UserWithdrawalLifecycle {

	@Id
	private String withdrawalId;

	@Indexed(name = "uk_withdrawal_lifecycle_user_id", unique = true)
	private String userId;

	private UserWithdrawalCleanupStatus status;
	private String firebaseProjectId;
	private String firebaseUid;
	private Instant requestedAt;
	private Instant updatedAt;
	private int attemptCount;
	private Instant nextAttemptAt;
	private String leaseOwner;
	private Instant leaseUntil;
	private String lastErrorCode;
	private Instant externalDeletedAt;
	private Instant identitiesReleasedAt;
	private Instant cleanedAt;

	@Version
	private Long version;

	private UserWithdrawalLifecycle() {
	}

	private UserWithdrawalLifecycle(
			String withdrawalId,
			String userId,
			String firebaseProjectId,
			String firebaseUid,
			Instant requestedAt
	) {
		this.withdrawalId = requireUuid(withdrawalId, "withdrawalId");
		this.userId = requireUuid(userId, "userId");
		this.firebaseProjectId = normalizeTarget(firebaseProjectId, "firebaseProjectId");
		this.firebaseUid = normalizeTarget(firebaseUid, "firebaseUid");
		if ((this.firebaseProjectId == null) != (this.firebaseUid == null)) {
			throw new IllegalArgumentException("Firebase withdrawal target must be complete");
		}
		this.status = UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_PENDING;
		this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt must not be null");
		this.updatedAt = requestedAt;
		this.attemptCount = 0;
		this.nextAttemptAt = requestedAt;
	}

	public static UserWithdrawalLifecycle create(
			String userId,
			String firebaseProjectId,
			String firebaseUid,
			Instant requestedAt
	) {
		return new UserWithdrawalLifecycle(
				UUID.randomUUID().toString(), userId, firebaseProjectId, firebaseUid, requestedAt
		);
	}

	public boolean hasSameTarget(String projectId, String uid) {
		return Objects.equals(firebaseProjectId, projectId) && Objects.equals(firebaseUid, uid);
	}

	private static String normalizeTarget(String value, String fieldName) {
		if (value == null) {
			return null;
		}
		if (value.isBlank() || !value.equals(value.trim()) || value.length() > 128) {
			throw new IllegalArgumentException(fieldName + " must be a valid opaque value");
		}
		return value;
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID");
		}
	}

	public String getWithdrawalId() { return withdrawalId; }
	public String getUserId() { return userId; }
	public UserWithdrawalCleanupStatus getStatus() { return status; }
	public String getFirebaseProjectId() { return firebaseProjectId; }
	public String getFirebaseUid() { return firebaseUid; }
	public Instant getRequestedAt() { return requestedAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseUntil() { return leaseUntil; }
	public String getLastErrorCode() { return lastErrorCode; }
	public Instant getExternalDeletedAt() { return externalDeletedAt; }
	public Instant getIdentitiesReleasedAt() { return identitiesReleasedAt; }
	public Instant getCleanedAt() { return cleanedAt; }
	public Long getVersion() { return version; }
}
