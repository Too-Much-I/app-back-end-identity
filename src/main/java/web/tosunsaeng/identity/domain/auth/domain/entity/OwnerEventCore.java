package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;

@Document(collection = "owner_event_cores")
@CompoundIndexes({
		@CompoundIndex(name = "uk_owner_event_user_merged_source",
				def = "{ 'sourceUserId': 1 }", unique = true,
				partialFilter = "{ 'eventType': 'USER_MERGED' }"),
		@CompoundIndex(name = "uk_owner_event_trial_lineage",
				def = "{ 'phoneRejoinLineageId': 1 }", unique = true,
				partialFilter = "{ 'eventType': 'TRIAL_OWNER_REBIND_APPROVED' }")
})
public class OwnerEventCore {

	public static final int SCHEMA_VERSION = 1;
	public static final String PRODUCER = "identity";
	public static final String PHONE_REJOIN = "PHONE_REJOIN";

	@Id
	private String eventId;
	private OwnerEventType eventType;
	private int schemaVersion;
	private String sourceUserId;
	private String targetUserId;
	private Instant occurredAt;
	@Field(write = Field.Write.ALWAYS) private String producer;
	@Field(write = Field.Write.ALWAYS) private String consumerScopeId;
	@Field(write = Field.Write.ALWAYS) private String lifecycleReason;
	@Field(write = Field.Write.ALWAYS) private Long sourceBindingRevision;
	@Field(write = Field.Write.ALWAYS) private Long targetBindingRevision;
	@Field(write = Field.Write.ALWAYS) private String phoneRejoinLineageId;
	private Set<OwnerEventConsumer> requiredConsumers;
	@Field(write = Field.Write.ALWAYS) private Instant allPublishedAt;
	@Indexed(name = "ttl_owner_event_core_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS) private Instant cleanupAt;
	private Instant createdAt;

	private OwnerEventCore() {}

	private OwnerEventCore(
			String eventId, OwnerEventType eventType, String sourceUserId, String targetUserId,
			Instant occurredAt, String producer, String consumerScopeId, String lifecycleReason,
			Long sourceBindingRevision, Long targetBindingRevision,
			String phoneRejoinLineageId, Set<OwnerEventConsumer> requiredConsumers
	) {
		this.eventId = requireUuid(eventId, "eventId");
		this.eventType = Objects.requireNonNull(eventType);
		this.schemaVersion = SCHEMA_VERSION;
		this.sourceUserId = requireUuid(sourceUserId, "sourceUserId");
		this.targetUserId = requireUuid(targetUserId, "targetUserId");
		if (this.sourceUserId.equals(this.targetUserId)) {
			throw new IllegalArgumentException("sourceUserId and targetUserId must differ");
		}
		this.occurredAt = Objects.requireNonNull(occurredAt);
		this.createdAt = occurredAt;
		this.producer = producer;
		this.consumerScopeId = consumerScopeId;
		this.lifecycleReason = lifecycleReason;
		this.sourceBindingRevision = sourceBindingRevision;
		this.targetBindingRevision = targetBindingRevision;
		this.phoneRejoinLineageId = phoneRejoinLineageId;
		this.requiredConsumers = Set.copyOf(Objects.requireNonNull(requiredConsumers));
		validateShape();
	}

	public static OwnerEventCore userMerged(
			String sourceUserId, String targetUserId, Instant occurredAt
	) {
		return new OwnerEventCore(UUID.randomUUID().toString(), OwnerEventType.USER_MERGED,
				sourceUserId, targetUserId, occurredAt, null, null, null, null, null, null,
				Set.of(OwnerEventConsumer.BILLING, OwnerEventConsumer.LEARNING_CORE));
	}

	public static OwnerEventCore trialOwnerRebindApproved(
			String sourceUserId, String targetUserId, String consumerScopeId,
			long sourceBindingRevision, long targetBindingRevision,
			String phoneRejoinLineageId, Instant occurredAt
	) {
		return new OwnerEventCore(UUID.randomUUID().toString(),
				OwnerEventType.TRIAL_OWNER_REBIND_APPROVED, sourceUserId, targetUserId,
				occurredAt, PRODUCER, requireScope(consumerScopeId), PHONE_REJOIN,
				requireRevision(sourceBindingRevision), requireRevision(targetBindingRevision),
				requireUuid(phoneRejoinLineageId, "phoneRejoinLineageId"),
				Set.of(OwnerEventConsumer.BILLING));
	}

	private void validateShape() {
		if (eventType == OwnerEventType.USER_MERGED) {
			if (producer != null || consumerScopeId != null || lifecycleReason != null
					|| sourceBindingRevision != null || targetBindingRevision != null
					|| phoneRejoinLineageId != null
					|| !requiredConsumers.equals(Set.of(
							OwnerEventConsumer.BILLING, OwnerEventConsumer.LEARNING_CORE))) {
				throw new IllegalArgumentException("USER_MERGED shape is invalid");
			}
		} else if (!PRODUCER.equals(producer) || !PHONE_REJOIN.equals(lifecycleReason)
				|| consumerScopeId == null || sourceBindingRevision == null
				|| targetBindingRevision == null || phoneRejoinLineageId == null
				|| !requiredConsumers.equals(Set.of(OwnerEventConsumer.BILLING))) {
			throw new IllegalArgumentException("TRIAL_OWNER_REBIND_APPROVED shape is invalid");
		}
	}

	private static Long requireRevision(long revision) {
		if (revision < 1 || revision > PhoneEligibilityBindingOutbox.MAX_BINDING_REVISION) {
			throw new IllegalArgumentException("binding revision is out of range");
		}
		return revision;
	}

	private static String requireScope(String value) {
		String required = Objects.requireNonNull(value);
		if (!required.matches("[A-Za-z0-9._:-]{1,128}")) {
			throw new IllegalArgumentException("consumerScopeId has an invalid format");
		}
		return required;
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID uuid = UUID.fromString(value);
			if (!uuid.toString().equals(value)) throw new IllegalArgumentException();
			return value;
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(fieldName + " must be a lowercase canonical UUID");
		}
	}

	public String getEventId() { return eventId; }
	public OwnerEventType getEventType() { return eventType; }
	public int getSchemaVersion() { return schemaVersion; }
	public String getSourceUserId() { return sourceUserId; }
	public String getTargetUserId() { return targetUserId; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getProducer() { return producer; }
	public String getConsumerScopeId() { return consumerScopeId; }
	public String getLifecycleReason() { return lifecycleReason; }
	public Long getSourceBindingRevision() { return sourceBindingRevision; }
	public Long getTargetBindingRevision() { return targetBindingRevision; }
	public String getPhoneRejoinLineageId() { return phoneRejoinLineageId; }
	public Set<OwnerEventConsumer> getRequiredConsumers() { return Set.copyOf(requiredConsumers); }
	public Instant getAllPublishedAt() { return allPublishedAt; }
	public Instant getCleanupAt() { return cleanupAt; }
	public Instant getCreatedAt() { return createdAt; }

	@Override public String toString() {
		return "OwnerEventCore[eventId=" + eventId + ", eventType=" + eventType + "]";
	}
}
