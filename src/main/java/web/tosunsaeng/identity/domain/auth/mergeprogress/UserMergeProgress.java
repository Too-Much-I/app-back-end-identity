package web.tosunsaeng.identity.domain.auth.mergeprogress;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.*;
import org.springframework.data.mongodb.core.mapping.Document;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;

@Getter
@Document("user_merge_progress")
@CompoundIndexes({
	@CompoundIndex(name="merge_target_created", def="{'targetUserId':1,'createdAt':-1,'_id':-1}"),
	@CompoundIndex(name="merge_target_active_created", def="{'targetUserId':1,'completedAt':1,'createdAt':-1,'_id':-1}")
})
public class UserMergeProgress {
	@Id private String mergeId;
	private String targetUserId;
	private String sourceUserId;
	private Instant createdAt;
	private MergeCompletionProfile completionProfile;
	private Set<OwnerEventConsumer> requiredConsumers;
	private Instant learningCoreConfirmedAt;
	private Instant billingConfirmedAt;
	private Instant completedAt;
	private boolean privacyCleanupRequested;
	private int progressContractVersion;
	private Instant updatedAt;
	@Version private Long version;
	@Indexed(name="merge_retention_review") private Instant retentionReviewAt;
	@Indexed(name="ttl_merge_progress", expireAfter="0s") private Instant cleanupAt;

	private UserMergeProgress() {}
	public static UserMergeProgress create(OwnerEventCore core, MergeCompletionProfile profile) {
		if (core.getProgressContractVersion() != 1 || !core.getRequiredConsumers().equals(profile.consumers()))
			throw new IllegalArgumentException("Tracked merge contract mismatch");
		UserMergeProgress p = new UserMergeProgress();
		p.mergeId = core.getEventId();
		p.targetUserId = core.getTargetUserId();
		p.sourceUserId = core.getSourceUserId();
		p.createdAt = core.getOccurredAt();
		p.updatedAt = p.createdAt;
		p.completionProfile = profile;
		p.requiredConsumers = profile.consumers();
		p.progressContractVersion = 1;
		p.retentionReviewAt = p.createdAt.plus(Duration.ofDays(90));
		p.validate();
		return p;
	}
	public Instant confirmedAt(OwnerEventConsumer consumer) {
		return consumer == OwnerEventConsumer.LEARNING_CORE ? learningCoreConfirmedAt : billingConfirmedAt;
	}
	public void confirm(OwnerEventConsumer consumer, Instant now) {
		validate();
		if (!requiredConsumers.contains(consumer) || now.isBefore(createdAt))
			throw new IllegalStateException("Invalid merge confirmation");
		if (confirmedAt(consumer) != null) return;
		if (consumer == OwnerEventConsumer.LEARNING_CORE) learningCoreConfirmedAt = now;
		else billingConfirmedAt = now;
		updatedAt = updatedAt.isAfter(now) ? updatedAt : now;
		if (requiredConsumers.stream().allMatch(c -> confirmedAt(c) != null)) {
			completedAt = requiredConsumers.stream().map(this::confirmedAt).max(Instant::compareTo).orElseThrow();
			cleanupAt = privacyCleanupRequested ? completedAt : completedAt.plus(Duration.ofDays(30));
			retentionReviewAt = null;
		}
		validate();
	}
	public void validate() {
		if (progressContractVersion != 1 || completionProfile == null
				|| !Objects.equals(requiredConsumers, completionProfile.consumers())
				|| mergeId == null || targetUserId == null || sourceUserId == null || createdAt == null || updatedAt == null
                || targetUserId.isBlank() || sourceUserId.isBlank() || targetUserId.equals(sourceUserId)
				|| (!requiredConsumers.contains(OwnerEventConsumer.BILLING) && billingConfirmedAt != null))
			throw new IllegalStateException("Invalid merge progress");
		if (requiredConsumers.stream().map(this::confirmedAt).filter(Objects::nonNull)
				.anyMatch(at -> at.isBefore(createdAt) || at.isAfter(updatedAt)))
			throw new IllegalStateException("Invalid merge confirmation time");
		if (completedAt != null && !completedAt.equals(requiredConsumers.stream().map(this::confirmedAt)
				.filter(Objects::nonNull).max(Instant::compareTo).orElse(null)))
			throw new IllegalStateException("Invalid merge completion time");
		boolean all = requiredConsumers.stream().allMatch(c -> confirmedAt(c) != null);
		if (all != (completedAt != null) || (completedAt == null && cleanupAt != null)
				|| (completedAt != null && (cleanupAt == null || (!privacyCleanupRequested && !Objects.equals(cleanupAt, completedAt.plus(Duration.ofDays(30)))))))
			throw new IllegalStateException("Invalid merge completion");
	}
	@Override public String toString() { return "UserMergeProgress[redacted]"; }
}
