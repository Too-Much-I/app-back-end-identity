package web.tosunsaeng.identity.domain.auth.domain.entity;

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

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneRejoinLineageStatus;

@Document(collection = "phone_rejoin_lineages")
@CompoundIndexes({
		@CompoundIndex(name = "uk_phone_rejoin_source_scope",
				def = "{ 'sourcePhoneIdentityId': 1, 'consumerScopeId': 1 }", unique = true),
		@CompoundIndex(name = "uk_phone_rejoin_claimed_event",
				def = "{ 'claimedEventId': 1 }", unique = true,
				partialFilter = "{ 'claimedEventId': { $type: 'string' } }"),
		@CompoundIndex(name = "ix_phone_rejoin_lookup",
				def = "{ 'status': 1, 'consumerScopeId': 1, 'releasedAt': 1 }"),
		@CompoundIndex(name = "ix_phone_rejoin_target_status",
				def = "{ 'targetUserId': 1, 'status': 1 }")
})
public class PhoneRejoinLineage {
	@Id private String lineageId;
	private String sourceWithdrawalId;
	private String sourceUserId;
	private String sourcePhoneIdentityId;
	private String consumerScopeId;
	private long sourceBindingRevision;
	private PhoneRejoinLineageStatus status;
	private Instant releasedAt;
	@Field(write = Field.Write.ALWAYS) private String claimedEventId;
	@Field(write = Field.Write.ALWAYS) private String targetUserId;
	@Field(write = Field.Write.ALWAYS) private Long targetBindingRevision;
	@Field(write = Field.Write.ALWAYS) private Instant consumedAt;
	@Field(write = Field.Write.ALWAYS) private String failureCode;
	@Field(write = Field.Write.ALWAYS) private Instant retentionReviewAt;
	@Indexed(name = "ttl_phone_rejoin_lineage_cleanup_at", expireAfter = "0s")
	@Field(write = Field.Write.ALWAYS) private Instant cleanupAt;
	@Version private Long version;

	private PhoneRejoinLineage() {}

	private PhoneRejoinLineage(
			String lineageId, String sourceWithdrawalId, String sourceUserId,
			String sourcePhoneIdentityId, String consumerScopeId,
			long sourceBindingRevision, Instant releasedAt
	) {
		this.lineageId = requireUuid(lineageId, "lineageId");
		this.sourceWithdrawalId = requireUuid(sourceWithdrawalId, "sourceWithdrawalId");
		this.sourceUserId = requireUuid(sourceUserId, "sourceUserId");
		this.sourcePhoneIdentityId = requireUuid(sourcePhoneIdentityId, "sourcePhoneIdentityId");
		this.consumerScopeId = requireScope(consumerScopeId);
		if (sourceBindingRevision < 1) throw new IllegalArgumentException("sourceBindingRevision must be positive");
		this.sourceBindingRevision = sourceBindingRevision;
		this.status = PhoneRejoinLineageStatus.AVAILABLE;
		this.releasedAt = Objects.requireNonNull(releasedAt);
	}

	public static PhoneRejoinLineage available(
			String withdrawalId, String sourceUserId, String sourcePhoneIdentityId,
			String consumerScopeId, long sourceBindingRevision, Instant releasedAt
	) {
		return new PhoneRejoinLineage(UUID.randomUUID().toString(), withdrawalId,
				sourceUserId, sourcePhoneIdentityId, consumerScopeId,
				sourceBindingRevision, releasedAt);
	}

	public void consume(String eventId, String targetUserId, long targetRevision, Instant at) {
		if (status != PhoneRejoinLineageStatus.AVAILABLE) {
			throw new IllegalStateException("Only AVAILABLE lineage can be consumed");
		}
		this.status = PhoneRejoinLineageStatus.CONSUMED;
		this.claimedEventId = requireUuid(eventId, "eventId");
		this.targetUserId = requireUuid(targetUserId, "targetUserId");
		if (sourceUserId.equals(this.targetUserId)) {
			throw new IllegalArgumentException("source and target must differ");
		}
		if (targetRevision < 1) throw new IllegalArgumentException("targetRevision must be positive");
		this.targetBindingRevision = targetRevision;
		this.consumedAt = Objects.requireNonNull(at);
	}

	public void requireReconciliation(String code, Instant at) {
		if (status != PhoneRejoinLineageStatus.AVAILABLE) return;
		this.status = PhoneRejoinLineageStatus.RECONCILIATION_REQUIRED;
		this.failureCode = requireCode(code);
		this.retentionReviewAt = Objects.requireNonNull(at);
	}

	public void markDeliveryPublished(Instant cleanupAt) {
		if (status != PhoneRejoinLineageStatus.CONSUMED || claimedEventId == null) {
			throw new IllegalStateException("Only consumed lineage can receive cleanupAt");
		}
		Instant required = Objects.requireNonNull(cleanupAt);
		if (consumedAt != null && !required.isAfter(consumedAt)) {
			throw new IllegalArgumentException("cleanupAt must be after consumedAt");
		}
		this.cleanupAt = required;
	}

	private static String requireCode(String value) {
		String code = Objects.requireNonNull(value);
		if (!code.matches("[A-Z0-9_]{1,64}")) throw new IllegalArgumentException("invalid failureCode");
		return code;
	}

	private static String requireScope(String value) {
		String scope = Objects.requireNonNull(value);
		if (!scope.matches("[A-Za-z0-9._:-]{1,128}")) throw new IllegalArgumentException("invalid scope");
		return scope;
	}

	private static String requireUuid(String value, String name) {
		try {
			UUID uuid = UUID.fromString(value);
			if (!uuid.toString().equals(value)) throw new IllegalArgumentException();
			return value;
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException(name + " must be a lowercase canonical UUID");
		}
	}

	public String getLineageId() { return lineageId; }
	public String getSourceWithdrawalId() { return sourceWithdrawalId; }
	public String getSourceUserId() { return sourceUserId; }
	public String getSourcePhoneIdentityId() { return sourcePhoneIdentityId; }
	public String getConsumerScopeId() { return consumerScopeId; }
	public long getSourceBindingRevision() { return sourceBindingRevision; }
	public PhoneRejoinLineageStatus getStatus() { return status; }
	public Instant getReleasedAt() { return releasedAt; }
	public String getClaimedEventId() { return claimedEventId; }
	public String getTargetUserId() { return targetUserId; }
	public Long getTargetBindingRevision() { return targetBindingRevision; }
	public Instant getConsumedAt() { return consumedAt; }
	public String getFailureCode() { return failureCode; }
	public Instant getRetentionReviewAt() { return retentionReviewAt; }
	public Instant getCleanupAt() { return cleanupAt; }
	public Long getVersion() { return version; }
}
