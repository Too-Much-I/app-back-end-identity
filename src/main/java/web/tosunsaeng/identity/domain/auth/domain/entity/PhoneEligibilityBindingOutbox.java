package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingEventType;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;

@Document(collection = "phone_eligibility_binding_outbox")
@CompoundIndexes({
		@CompoundIndex(
				name = "uk_phone_eligibility_outbox_user_scope_revision",
				def = "{ 'userId': 1, 'consumerScopeId': 1, 'bindingRevision': 1 }",
				unique = true
		),
		@CompoundIndex(
				name = "ix_phone_eligibility_outbox_due",
				def = "{ 'consumerScopeId': 1, 'status': 1, 'nextAttemptAt': 1, 'occurredAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_phone_eligibility_outbox_expired_lease",
				def = "{ 'consumerScopeId': 1, 'status': 1, 'leaseExpiresAt': 1, 'occurredAt': 1 }"
		),
		@CompoundIndex(
				name = "ix_phone_eligibility_outbox_dead_letter_review",
				def = "{ 'status': 1, 'retentionReviewAt': 1 }"
		)
})
public class PhoneEligibilityBindingOutbox {

	public static final int SCHEMA_VERSION = 1;
	public static final String PRODUCER = "identity";
	public static final long MAX_BINDING_REVISION = 9_007_199_254_740_991L;
	private static final int MAX_CANDIDATES = 8;

	@Id
	private String eventId;

	private PhoneEligibilityBindingEventType eventType;

	private int schemaVersion;

	private String producer;

	private String userId;

	private String consumerScopeId;

	private long bindingRevision;

	private List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates;

	private PhoneEligibilityBindingOutboxStatus status;

	@Field(write = Field.Write.ALWAYS)
	private Instant verifiedAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant revokedAt;

	private Instant occurredAt;

	@Field(write = Field.Write.ALWAYS)
	private String leaseOwner;

	@Field(write = Field.Write.ALWAYS)
	private Instant leaseExpiresAt;

	private int attemptCount;

	private Instant nextAttemptAt;

	@Field(write = Field.Write.ALWAYS)
	private PhoneEligibilityBindingFailureCode lastFailureCode;

	@Field(write = Field.Write.ALWAYS)
	private Instant publishedAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant deadLetteredAt;

	@Field(write = Field.Write.ALWAYS)
	private Instant retentionReviewAt;

	@Indexed(name = "ttl_phone_eligibility_outbox_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS)
	private Instant cleanupAt;

	private PhoneEligibilityBindingOutbox() {
	}

	private PhoneEligibilityBindingOutbox(
			String eventId,
			PhoneEligibilityBindingEventType eventType,
			String userId,
			String consumerScopeId,
			long bindingRevision,
			List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates,
			Instant verifiedAt,
			Instant revokedAt,
			Instant occurredAt
	) {
		this.eventId = requireUuid(eventId, "eventId");
		this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
		this.schemaVersion = SCHEMA_VERSION;
		this.producer = PRODUCER;
		this.userId = requireUuid(userId, "userId");
		this.consumerScopeId = requireScope(consumerScopeId);
		this.bindingRevision = requireRevision(bindingRevision);
		this.fingerprintCandidates = requireCandidates(eventType, fingerprintCandidates);
		this.verifiedAt = requireEventTime(
				eventType == PhoneEligibilityBindingEventType.VERIFIED,
				verifiedAt,
				"verifiedAt"
		);
		this.revokedAt = requireEventTime(
				eventType == PhoneEligibilityBindingEventType.REVOKED,
				revokedAt,
				"revokedAt"
		);
		this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		if (this.verifiedAt != null && this.verifiedAt.isAfter(this.occurredAt)) {
			throw new IllegalArgumentException("verifiedAt must not be after occurredAt");
		}
		if (this.revokedAt != null && this.revokedAt.isAfter(this.occurredAt)) {
			throw new IllegalArgumentException("revokedAt must not be after occurredAt");
		}
		this.status = PhoneEligibilityBindingOutboxStatus.PENDING;
		this.attemptCount = 0;
		this.nextAttemptAt = this.occurredAt;
	}

	public static PhoneEligibilityBindingOutbox createVerified(
			String userId,
			String consumerScopeId,
			long bindingRevision,
			List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates,
			Instant verifiedAt,
			Instant occurredAt
	) {
		return new PhoneEligibilityBindingOutbox(
				UUID.randomUUID().toString(),
				PhoneEligibilityBindingEventType.VERIFIED,
				userId,
				consumerScopeId,
				bindingRevision,
				fingerprintCandidates,
				verifiedAt,
				null,
				occurredAt
		);
	}

	public static PhoneEligibilityBindingOutbox createRevoked(
			String userId,
			String consumerScopeId,
			long bindingRevision,
			Instant revokedAt,
			Instant occurredAt
	) {
		return new PhoneEligibilityBindingOutbox(
				UUID.randomUUID().toString(),
				PhoneEligibilityBindingEventType.REVOKED,
				userId,
				consumerScopeId,
				bindingRevision,
				List.of(),
				null,
				revokedAt,
				occurredAt
		);
	}

	private static List<PhoneEligibilityFingerprintCandidate> requireCandidates(
			PhoneEligibilityBindingEventType eventType,
			List<PhoneEligibilityFingerprintCandidate> candidates
	) {
		List<PhoneEligibilityFingerprintCandidate> required = List.copyOf(
				Objects.requireNonNull(candidates, "fingerprintCandidates must not be null")
		);
		if (eventType == PhoneEligibilityBindingEventType.REVOKED) {
			if (!required.isEmpty()) {
				throw new IllegalArgumentException("Revoked event must not have candidates");
			}
			return List.of();
		}
		if (required.isEmpty() || required.size() > MAX_CANDIDATES) {
			throw new IllegalArgumentException("Verified event must have 1 to 8 candidates");
		}
		Set<String> versions = new HashSet<>();
		Set<String> pairs = new HashSet<>();
		for (PhoneEligibilityFingerprintCandidate candidate : required) {
			String keyVersion = candidate.keyVersion();
			String value = candidate.fingerprint();
			if (!keyVersion.matches("[A-Za-z0-9._-]{1,32}")) {
				throw new IllegalArgumentException("candidate keyVersion has an invalid format");
			}
			if (!value.matches("[A-Za-z0-9_-]{43}")) {
				throw new IllegalArgumentException("candidate value has an invalid format");
			}
			if (!versions.add(keyVersion) || !pairs.add(keyVersion + '\u0000' + value)) {
				throw new IllegalArgumentException("candidate versions must be unique");
			}
		}
		return required.stream()
				.sorted(Comparator.comparing(PhoneEligibilityFingerprintCandidate::keyVersion)
						.thenComparing(PhoneEligibilityFingerprintCandidate::fingerprint))
				.toList();
	}

	private static Instant requireEventTime(boolean required, Instant value, String fieldName) {
		if (required) {
			return Objects.requireNonNull(value, fieldName + " must not be null");
		}
		if (value != null) {
			throw new IllegalArgumentException(fieldName + " must be null for this event type");
		}
		return null;
	}

	private static long requireRevision(long value) {
		if (value < 1 || value > MAX_BINDING_REVISION) {
			throw new IllegalArgumentException("bindingRevision is out of range");
		}
		return value;
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equals(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a lowercase canonical UUID.");
		}
	}

	private static String requireScope(String value) {
		String required = Objects.requireNonNull(value, "consumerScopeId must not be null");
		if (!required.matches("[A-Za-z0-9._:-]{1,128}")) {
			throw new IllegalArgumentException("consumerScopeId has an invalid format");
		}
		return required;
	}

	public String getEventId() { return eventId; }
	public PhoneEligibilityBindingEventType getEventType() { return eventType; }
	public int getSchemaVersion() { return schemaVersion; }
	public String getProducer() { return producer; }
	public String getUserId() { return userId; }
	public String getConsumerScopeId() { return consumerScopeId; }
	public long getBindingRevision() { return bindingRevision; }
	public List<PhoneEligibilityFingerprintCandidate> getFingerprintCandidates() {
		return List.copyOf(fingerprintCandidates);
	}
	public PhoneEligibilityBindingOutboxStatus getStatus() { return status; }
	public Instant getVerifiedAt() { return verifiedAt; }
	public Instant getRevokedAt() { return revokedAt; }
	public Instant getOccurredAt() { return occurredAt; }
	public String getLeaseOwner() { return leaseOwner; }
	public Instant getLeaseExpiresAt() { return leaseExpiresAt; }
	public int getAttemptCount() { return attemptCount; }
	public Instant getNextAttemptAt() { return nextAttemptAt; }
	public PhoneEligibilityBindingFailureCode getLastFailureCode() { return lastFailureCode; }
	public Instant getPublishedAt() { return publishedAt; }
	public Instant getDeadLetteredAt() { return deadLetteredAt; }
	public Instant getRetentionReviewAt() { return retentionReviewAt; }
	public Instant getCleanupAt() { return cleanupAt; }

	@Override
	public String toString() {
		return "PhoneEligibilityBindingOutbox[eventId=" + eventId
				+ ", eventType=" + eventType
				+ ", schemaVersion=" + schemaVersion
				+ ", consumerScopeId=" + consumerScopeId
				+ ", bindingRevision=" + bindingRevision
				+ ", fingerprintCandidates=[REDACTED], status=" + status + "]";
	}
}
