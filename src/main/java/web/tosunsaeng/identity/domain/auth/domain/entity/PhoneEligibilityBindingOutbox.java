package web.tosunsaeng.identity.domain.auth.domain.entity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;

@Document(collection = "phone_eligibility_binding_outbox")
@CompoundIndexes({
		@CompoundIndex(
				name = "ix_phone_eligibility_binding_user_scope_created_at",
				def = "{ 'userId': 1, 'consumerScopeId': 1, 'createdAt': -1 }"
		),
		@CompoundIndex(
				name = "ix_phone_eligibility_outbox_status_created_at",
				def = "{ 'status': 1, 'createdAt': 1 }"
		)
})
public class PhoneEligibilityBindingOutbox {

	@Id
	private String eventId;

	private String userId;

	private String consumerScopeId;

	private List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates;

	private PhoneEligibilityBindingOutboxStatus status;

	private Instant verifiedAt;

	private Instant createdAt;

	private Instant publishedAt;

	private PhoneEligibilityBindingOutbox() {
	}

	private PhoneEligibilityBindingOutbox(
			String eventId,
			String userId,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates,
			PhoneEligibilityBindingOutboxStatus status,
			Instant verifiedAt,
			Instant createdAt,
			Instant publishedAt
	) {
		this.eventId = requireUuid(eventId, "eventId");
		this.userId = requireUuid(userId, "userId");
		this.consumerScopeId = requireScope(consumerScopeId);
		this.fingerprintCandidates = List.copyOf(Objects.requireNonNull(
				fingerprintCandidates,
				"fingerprintCandidates must not be null"
		));
		if (this.fingerprintCandidates.isEmpty()) {
			throw new IllegalArgumentException("fingerprintCandidates must not be empty");
		}
		this.status = Objects.requireNonNull(status, "status must not be null");
		this.verifiedAt = Objects.requireNonNull(verifiedAt, "verifiedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.publishedAt = publishedAt;
	}

	public static PhoneEligibilityBindingOutbox create(
			String userId,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> fingerprintCandidates,
			Instant verifiedAt
	) {
		return new PhoneEligibilityBindingOutbox(
				UUID.randomUUID().toString(),
				userId,
				consumerScopeId,
				fingerprintCandidates,
				PhoneEligibilityBindingOutboxStatus.PENDING,
				verifiedAt,
				verifiedAt,
				null
		);
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID.");
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
	public String getUserId() { return userId; }
	public String getConsumerScopeId() { return consumerScopeId; }
	public List<PhoneEligibilityFingerprintCandidate> getFingerprintCandidates() {
		return List.copyOf(fingerprintCandidates);
	}
	public PhoneEligibilityBindingOutboxStatus getStatus() { return status; }
	public Instant getVerifiedAt() { return verifiedAt; }
	public Instant getCreatedAt() { return createdAt; }
	public Instant getPublishedAt() { return publishedAt; }

	@Override
	public String toString() {
		return "PhoneEligibilityBindingOutbox[eventId=" + eventId
				+ ", userId=" + userId
				+ ", consumerScopeId=" + consumerScopeId
				+ ", fingerprintCandidates=[REDACTED], status=" + status + "]";
	}
}
