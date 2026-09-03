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

import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;

@Document(collection = "owner_event_deliveries")
@CompoundIndexes({
		@CompoundIndex(name = "uk_owner_delivery_event_consumer",
				def = "{ 'eventId': 1, 'consumer': 1 }", unique = true),
		@CompoundIndex(name = "uk_owner_delivery_consumer_sequence",
				def = "{ 'consumer': 1, 'consumerSequence': 1 }", unique = true),
		@CompoundIndex(name = "ix_owner_delivery_due",
				def = "{ 'consumer': 1, 'status': 1, 'nextAttemptAt': 1, 'consumerSequence': 1 }"),
		@CompoundIndex(name = "ix_owner_delivery_expired_lease",
				def = "{ 'consumer': 1, 'status': 1, 'leaseExpiresAt': 1, 'consumerSequence': 1 }"),
		@CompoundIndex(name = "ix_owner_delivery_dead_letter_review",
				def = "{ 'status': 1, 'retentionReviewAt': 1 }")
})
public class OwnerEventDelivery {
	@Id private String deliveryId;
	private String eventId;
	private OwnerEventConsumer consumer;
	private long consumerSequence;
	private OwnerEventDeliveryStatus status;
	private int attemptCount;
	private Instant nextAttemptAt;
	@Field(write = Field.Write.ALWAYS) private String leaseOwner;
	@Field(write = Field.Write.ALWAYS) private Instant leaseExpiresAt;
	@Field(write = Field.Write.ALWAYS) private OwnerEventFailureCode lastFailureCode;
	@Field(write = Field.Write.ALWAYS) private Instant publishedAt;
	@Field(write = Field.Write.ALWAYS) private Instant deadLetteredAt;
	@Field(write = Field.Write.ALWAYS) private Instant retentionReviewAt;
	@Indexed(name = "ttl_owner_event_delivery_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS) private Instant cleanupAt;
	private Instant createdAt;

	private OwnerEventDelivery() {}

	private OwnerEventDelivery(
			String deliveryId, String eventId, OwnerEventConsumer consumer,
			long consumerSequence, Instant createdAt
	) {
		this.deliveryId = requireUuid(deliveryId);
		this.eventId = requireUuid(eventId);
		this.consumer = Objects.requireNonNull(consumer);
		if (consumerSequence < 1) throw new IllegalArgumentException("consumerSequence must be positive");
		this.consumerSequence = consumerSequence;
		this.status = OwnerEventDeliveryStatus.PENDING;
		this.attemptCount = 0;
		this.createdAt = Objects.requireNonNull(createdAt);
		this.nextAttemptAt = createdAt;
	}

	public static OwnerEventDelivery create(
			String eventId, OwnerEventConsumer consumer, long sequence, Instant createdAt
	) {
		return new OwnerEventDelivery(UUID.randomUUID().toString(), eventId, consumer, sequence, createdAt);
	}

	private static String requireUuid(String value) {
		try {
			UUID uuid = UUID.fromString(value);
			if (!uuid.toString().equals(value)) throw new IllegalArgumentException();
			return value;
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("identifier must be a lowercase canonical UUID");
		}
	}

	public String getDeliveryId() { return deliveryId; }
	public String getEventId() { return eventId; }
	public OwnerEventConsumer getConsumer() { return consumer; }
	public long getConsumerSequence() { return consumerSequence; }
	public OwnerEventDeliveryStatus getStatus() { return status; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
	public OwnerEventFailureCode getLastFailureCode() { return lastFailureCode; }
	public Instant getPublishedAt() { return publishedAt; }
	public Instant getDeadLetteredAt() { return deadLetteredAt; }
	public Instant getRetentionReviewAt() { return retentionReviewAt; }
	public Instant getCleanupAt() { return cleanupAt; }
	public Instant getCreatedAt() { return createdAt; }
}
