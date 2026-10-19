package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedOutboxStatus;

@Document(collection = "user_merged_outbox")
@CompoundIndexes({
		@CompoundIndex(
				name = "ix_user_merged_outbox_due",
				def = "{ 'status': 1, 'nextAttemptAt': 1, 'occurredAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_user_merged_outbox_expired_lease",
				def = "{ 'status': 1, 'leaseExpiresAt': 1, 'occurredAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_user_merged_outbox_dead_letter_review",
				def = "{ 'status': 1, 'retentionReviewAt': 1 }"
		)
})
public class UserMergedOutbox {

	public static final int SCHEMA_VERSION = 1;

	@Id
	private String eventId;

	private int schemaVersion;

	@Indexed(name = "uk_user_merged_outbox_source_user_id", unique = true)
	private String sourceUserId;

	private String targetUserId;

	private Instant occurredAt;

	private UserMergedOutboxStatus status;

	@Field(write = Field.Write.ALWAYS)
	private String leaseOwner;

	@Field(write = Field.Write.ALWAYS)
	private Instant leaseExpiresAt;

	private int attemptCount;

	private Instant nextAttemptAt;

	@Field(write = Field.Write.ALWAYS)
	private UserMergedFailureCode lastFailureCode;

	@Field(write = Field.Write.ALWAYS)
	private Instant publishedAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant deadLetteredAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant retentionReviewAt;

	@Indexed(name = "ttl_user_merged_outbox_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS)
	private Instant cleanupAt;

	private UserMergedOutbox() {
	}

	private UserMergedOutbox(
			String eventId,
			String sourceUserId,
			String targetUserId,
			Instant occurredAt
	) {
		this.eventId = requireUuid(eventId, "eventId");
		this.schemaVersion = SCHEMA_VERSION;
		this.sourceUserId = requireUuid(sourceUserId, "sourceUserId");
		this.targetUserId = requireUuid(targetUserId, "targetUserId");
		if (this.sourceUserId.equals(this.targetUserId)) {
			throw new IllegalArgumentException("sourceUserId and targetUserId must differ");
		}
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		this.status = UserMergedOutboxStatus.PENDING;
		this.attemptCount = 0;
		this.nextAttemptAt = this.occurredAt;
	}

	public static UserMergedOutbox create(
			String sourceUserId,
			String targetUserId,
			Instant occurredAt
	) {
		return new UserMergedOutbox(
				UUID.randomUUID().toString(),
				sourceUserId,
				targetUserId,
				occurredAt
		);
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equals(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(
					fieldName + " must be a lowercase canonical UUID."
			);
		}
	}

	public String getEventId() { return eventId; }
	public int getSchemaVersion() { return schemaVersion; }
	public String getSourceUserId() { return sourceUserId; }
	public String getTargetUserId() { return targetUserId; }
	public Instant getOccurredAt() { return occurredAt; }
	public UserMergedOutboxStatus getStatus() { return status; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public UserMergedFailureCode getLastFailureCode() { return lastFailureCode; }
	public Instant getPublishedAt() { return publishedAt; }
	public Instant getDeadLetteredAt() { return deadLetteredAt; }
	public Instant getRetentionReviewAt() { return retentionReviewAt; }
	public Instant getCleanupAt() { return cleanupAt; }

	@Override
	public String toString() {
		return "UserMergedOutbox[eventId=" + eventId
				+ ", schemaVersion=" + schemaVersion
				+ ", status=" + status + "]";
	}
}
