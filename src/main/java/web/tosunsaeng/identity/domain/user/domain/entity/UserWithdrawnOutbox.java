package web.tosunsaeng.identity.domain.user.domain.entity;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnFailureCode;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnOutboxStatus;

@Document(collection = "user_withdrawn_outbox")
@CompoundIndexes({
		@CompoundIndex(
				name = "ix_user_withdrawn_outbox_due",
				def = "{ 'status': 1, 'nextAttemptAt': 1, 'withdrawnAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_user_withdrawn_outbox_expired_lease",
				def = "{ 'status': 1, 'leaseExpiresAt': 1, 'withdrawnAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_user_withdrawn_outbox_dead_letter_review",
				def = "{ 'status': 1, 'retentionReviewAt': 1 }"
		)
})
public class UserWithdrawnOutbox {

	public static final int SCHEMA_VERSION = 1;
	private static final String BACKFILL_EVENT_NAMESPACE = "user-withdrawn:v1:";

	@Id
	private String eventId;

	private int schemaVersion;

	@Indexed(name = "uk_user_withdrawn_outbox_user_id", unique = true)
	private String userId;

	private Instant withdrawnAt;

	private UserWithdrawnOutboxStatus status;

	@Field(write = Field.Write.ALWAYS)
	private String leaseOwner;

	@Field(write = Field.Write.ALWAYS)
	private Instant leaseExpiresAt;

	private int attemptCount;

	private Instant nextAttemptAt;

	@Field(write = Field.Write.ALWAYS)
	private UserWithdrawnFailureCode lastFailureCode;

	@Field(write = Field.Write.ALWAYS)
	private Instant publishedAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant deadLetteredAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant retentionReviewAt;

	@Indexed(name = "ttl_user_withdrawn_outbox_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS)
	private Instant cleanupAt;

	private UserWithdrawnOutbox() {
	}

	private UserWithdrawnOutbox(
			String eventId,
			String userId,
			Instant withdrawnAt
	) {
		this.eventId = requireUuid(eventId, "eventId");
		this.schemaVersion = SCHEMA_VERSION;
		this.userId = requireUuid(userId, "userId");
		this.withdrawnAt = Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");
		this.status = UserWithdrawnOutboxStatus.PENDING;
		this.attemptCount = 0;
		this.nextAttemptAt = this.withdrawnAt;
	}

	public static UserWithdrawnOutbox create(String userId, Instant withdrawnAt) {
		return new UserWithdrawnOutbox(UUID.randomUUID().toString(), userId, withdrawnAt);
	}

	public static UserWithdrawnOutbox createBackfill(String userId, Instant withdrawnAt) {
		String requiredUserId = requireUuid(userId, "userId");
		Instant requiredWithdrawnAt = Objects.requireNonNull(
				withdrawnAt,
				"withdrawnAt must not be null"
		);
		String seed = BACKFILL_EVENT_NAMESPACE + requiredUserId + ":" + requiredWithdrawnAt;
		String eventId = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
		return new UserWithdrawnOutbox(eventId, requiredUserId, requiredWithdrawnAt);
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
	public String getUserId() { return userId; }
	public Instant getWithdrawnAt() { return withdrawnAt; }
	public UserWithdrawnOutboxStatus getStatus() { return status; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public UserWithdrawnFailureCode getLastFailureCode() { return lastFailureCode; }
	public Instant getPublishedAt() { return publishedAt; }
	public Instant getDeadLetteredAt() { return deadLetteredAt; }
	public Instant getRetentionReviewAt() { return retentionReviewAt; }
	public Instant getCleanupAt() { return cleanupAt; }

	@Override
	public String toString() {
		return "UserWithdrawnOutbox[eventId=[REDACTED], schemaVersion=" + schemaVersion
				+ ", status=" + status + "]";
	}
}
