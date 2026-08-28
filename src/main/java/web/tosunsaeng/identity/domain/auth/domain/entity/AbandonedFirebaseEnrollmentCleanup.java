package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentCleanupStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.AbandonedFirebaseEnrollmentFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;

@Document(collection = "abandoned_firebase_enrollment_cleanups")
@CompoundIndexes({
		@CompoundIndex(
				name = "uk_abandoned_firebase_cleanup_target",
				def = "{ 'firebaseProjectId': 1, 'firebaseUid': 1 }",
				unique = true
		),
		@CompoundIndex(
				name = "idx_abandoned_firebase_cleanup_grace",
				def = "{ 'status': 1, 'graceUntil': 1, 'updatedAt': 1 }"
		),
		@CompoundIndex(
				name = "idx_abandoned_firebase_cleanup_retry_lease",
				def = "{ 'status': 1, 'nextAttemptAt': 1, 'leaseUntil': 1 }"
		)
})
public class AbandonedFirebaseEnrollmentCleanup {

	private static final int MAX_OPAQUE_LENGTH = 128;

	@Id
	private String cleanupId;
	private String firebaseProjectId;
	private String firebaseUid;
	private long generation;
	private FirebaseEnrollmentBindingType bindingType;
	@Field(write = Field.Write.ALWAYS)
	private String boundUserId;
	private String sourceEnrollmentId;
	private AbandonedFirebaseEnrollmentCleanupStatus status;
	private Instant graceUntil;
	private Instant nextAttemptAt;
	private int attemptCount;
	private String leaseOwner;
	private Instant leaseUntil;
	private AbandonedFirebaseEnrollmentFailureCode lastErrorCode;
	private Instant lastActivityAt;
	private Instant externalDeletedAt;
	private Instant completedAt;
	@Indexed(name = "ttl_abandoned_firebase_cleanup_at", expireAfter = "0s")
	private Instant cleanupAt;
	private Instant createdAt;
	private Instant updatedAt;
	@Version
	private Long version;

	private AbandonedFirebaseEnrollmentCleanup() {
	}

	private AbandonedFirebaseEnrollmentCleanup(
			String cleanupId,
			FirebaseEnrollmentAttempt attempt,
			long generation,
			AbandonedFirebaseEnrollmentCleanupStatus status,
			Instant graceUntil,
			Instant lastActivityAt,
			Instant completedAt,
			Instant cleanupAt,
			Instant createdAt,
			Instant updatedAt
	) {
		FirebaseEnrollmentAttempt requiredAttempt = Objects.requireNonNull(attempt);
		this.cleanupId = requireUuid(cleanupId, "cleanupId");
		this.firebaseProjectId = requireOpaque(
				requiredAttempt.getFirebaseProjectId(), "firebaseProjectId"
		);
		this.firebaseUid = requireOpaque(requiredAttempt.getFirebaseUid(), "firebaseUid");
		if (generation < 1) throw new IllegalArgumentException("generation must be positive");
		this.generation = generation;
		this.bindingType = Objects.requireNonNull(requiredAttempt.getBindingType());
		this.boundUserId = requiredAttempt.getBoundUserId();
		this.sourceEnrollmentId = requireUuid(
				requiredAttempt.getEnrollmentId(), "sourceEnrollmentId"
		);
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.graceUntil = Objects.requireNonNull(graceUntil, "graceUntil must not be null");
		this.lastActivityAt = Objects.requireNonNull(
				lastActivityAt, "lastActivityAt must not be null"
		);
		this.completedAt = completedAt;
		this.cleanupAt = cleanupAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		validateTerminalFields();
	}

	public static AbandonedFirebaseEnrollmentCleanup createResumable(
			FirebaseEnrollmentAttempt attempt,
			Duration grace,
			Instant createdAt
	) {
		FirebaseEnrollmentAttempt requiredAttempt = Objects.requireNonNull(attempt);
		Instant requiredCreatedAt = Objects.requireNonNull(createdAt);
		return new AbandonedFirebaseEnrollmentCleanup(
				UUID.randomUUID().toString(),
				requiredAttempt,
				1,
				AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE,
				plus(requiredAttempt.getExpiresAt(), grace, "grace"),
				requiredCreatedAt,
				null,
				null,
				requiredCreatedAt,
				requiredCreatedAt
		);
	}

	public static AbandonedFirebaseEnrollmentCleanup createFinalized(
			FirebaseEnrollmentAttempt attempt,
			Instant terminalAt,
			Duration terminalRetention
	) {
		FirebaseEnrollmentAttempt requiredAttempt = Objects.requireNonNull(attempt);
		Instant requiredTerminalAt = Objects.requireNonNull(terminalAt);
		return new AbandonedFirebaseEnrollmentCleanup(
				UUID.randomUUID().toString(),
				requiredAttempt,
				1,
				AbandonedFirebaseEnrollmentCleanupStatus.FINALIZED,
				requiredAttempt.getExpiresAt(),
				requiredTerminalAt,
				requiredTerminalAt,
				plus(requiredTerminalAt, terminalRetention, "terminalRetention"),
				requiredTerminalAt,
				requiredTerminalAt
		);
	}

	public void resume(
			FirebaseEnrollmentAttempt attempt,
			Duration grace,
			Instant resumedAt
	) {
		FirebaseEnrollmentAttempt requiredAttempt = Objects.requireNonNull(attempt);
		Instant requiredResumedAt = Objects.requireNonNull(resumedAt);
		if (status != AbandonedFirebaseEnrollmentCleanupStatus.RESUMABLE) {
			throw new IllegalStateException("Only a resumable cleanup can start a new generation.");
		}
		if (!firebaseProjectId.equals(requiredAttempt.getFirebaseProjectId())
				|| !firebaseUid.equals(requiredAttempt.getFirebaseUid())
				|| bindingType != requiredAttempt.getBindingType()
				|| !Objects.equals(boundUserId, requiredAttempt.getBoundUserId())) {
			throw new IllegalStateException("Enrollment binding changed during cleanup coordination.");
		}
		generation = Math.addExact(generation, 1L);
		sourceEnrollmentId = requireUuid(requiredAttempt.getEnrollmentId(), "sourceEnrollmentId");
		graceUntil = plus(requiredAttempt.getExpiresAt(), grace, "grace");
		lastActivityAt = requiredResumedAt;
		updatedAt = requiredResumedAt;
		nextAttemptAt = null;
		lastErrorCode = null;
		cleanupAt = null;
	}

	private void validateTerminalFields() {
		boolean terminal = status == AbandonedFirebaseEnrollmentCleanupStatus.FINALIZED
				|| status == AbandonedFirebaseEnrollmentCleanupStatus.CLEANED;
		if (terminal != (completedAt != null) || terminal != (cleanupAt != null)) {
			throw new IllegalArgumentException("Only terminal cleanup records may expire.");
		}
		if (cleanupAt != null && !cleanupAt.isAfter(completedAt)) {
			throw new IllegalArgumentException("cleanupAt must be after completedAt");
		}
	}

	private static Instant plus(Instant base, Duration duration, String fieldName) {
		Duration required = Objects.requireNonNull(duration, fieldName + " must not be null");
		if (required.isZero() || required.isNegative()) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		try {
			return Objects.requireNonNull(base).plus(required);
		} catch (DateTimeException | ArithmeticException exception) {
			throw new IllegalArgumentException(fieldName + " is out of range");
		}
	}

	private static String requireOpaque(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank() || !required.equals(required.trim())
				|| required.length() > MAX_OPAQUE_LENGTH) {
			throw new IllegalArgumentException(fieldName + " must be a valid opaque value");
		}
		return required;
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID");
		}
	}

	public String getCleanupId() { return cleanupId; }
	public String getFirebaseProjectId() { return firebaseProjectId; }
	public String getFirebaseUid() { return firebaseUid; }
	public long getGeneration() { return generation; }
	public FirebaseEnrollmentBindingType getBindingType() { return bindingType; }
	public String getBoundUserId() { return boundUserId; }
	public String getSourceEnrollmentId() { return sourceEnrollmentId; }
	public AbandonedFirebaseEnrollmentCleanupStatus getStatus() { return status; }
	public Instant getGraceUntil() { return graceUntil; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public int getAttemptCount() { return attemptCount; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseUntil() { return leaseUntil; }
	public AbandonedFirebaseEnrollmentFailureCode getLastErrorCode() { return lastErrorCode; }
	public Instant getLastActivityAt() { return lastActivityAt; }
	public Instant getExternalDeletedAt() { return externalDeletedAt; }
	public Instant getCompletedAt() { return completedAt; }
	public Instant getCleanupAt() { return cleanupAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getUpdatedAt() { return updatedAt; }
	public Long getVersion() { return version; }

	@Override
	public String toString() {
		return "AbandonedFirebaseEnrollmentCleanup[status=" + status
				+ ", generation=" + generation
				+ ", bindingType=" + bindingType
				+ ", attemptCount=" + attemptCount
				+ ", identifiers=[REDACTED]]";
	}
}
