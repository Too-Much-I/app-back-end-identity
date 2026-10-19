package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneRejoinLineageStatus;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class PhoneRejoinLineageResolver {
	private final OwnerEventProperties properties;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneRejoinLineageRepository lineageRepository;
	private final PhoneEligibilityBindingRevisionRepository revisionRepository;
	private final UserRepository userRepository;
	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final OwnerEventCaptureService captureService;

	public PhoneRejoinLineageResolver(
			OwnerEventProperties properties,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneRejoinLineageRepository lineageRepository,
			PhoneEligibilityBindingRevisionRepository revisionRepository,
			UserRepository userRepository,
			UserWithdrawalLifecycleRepository lifecycleRepository,
			OwnerEventCaptureService captureService
	) {
		this.properties = Objects.requireNonNull(properties);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.lineageRepository = Objects.requireNonNull(lineageRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
		this.captureService = Objects.requireNonNull(captureService);
	}

	public OwnerEventCore resolve(
			PhoneFingerprintSet fingerprints, String consumerScopeId,
			String targetUserId, Instant resolvedAt
	) {
		if (!properties.isTrialRebindCaptureEnabled()) return null;
		Set<String> sourcePhoneIds = aliasRepository.findAllByFingerprintsAndStatus(
				Objects.requireNonNull(fingerprints).retained(), PhoneFingerprintAliasStatus.RELEASED)
				.stream().map(PhoneFingerprintAlias::getPhoneIdentityId)
				.collect(Collectors.toUnmodifiableSet());
		if (sourcePhoneIds.isEmpty()) return null;
		List<PhoneRejoinLineage> candidates = lineageRepository
				.findAllBySourcePhoneIdentityIdInAndConsumerScopeIdAndStatus(
						sourcePhoneIds, consumerScopeId, PhoneRejoinLineageStatus.AVAILABLE);
		if (candidates.isEmpty()) return null;
		if (candidates.size() != 1) {
			candidates.forEach(candidate -> candidate.requireReconciliation(
					"MULTIPLE_AVAILABLE_PREDECESSORS", resolvedAt));
			lineageRepository.saveAll(candidates);
			return null;
		}
		PhoneRejoinLineage lineage = candidates.getFirst();
		PhoneEligibilityBindingRevision sourceRevision = revisionRepository
				.findByUserIdAndConsumerScopeId(lineage.getSourceUserId(), consumerScopeId)
				.orElse(null);
		PhoneEligibilityBindingRevision targetRevision = revisionRepository
				.findByUserIdAndConsumerScopeId(targetUserId, consumerScopeId)
				.orElse(null);
		boolean valid = userRepository.findById(lineage.getSourceUserId())
				.filter(user -> user.getStatus() == UserStatus.WITHDRAWN).isPresent()
				&& userRepository.findById(targetUserId)
				.filter(user -> user.getStatus() == UserStatus.ACTIVE && user.isMember()).isPresent()
				&& lifecycleRepository.findById(lineage.getSourceWithdrawalId())
				.filter(lifecycle -> lifecycle.getStatus() == UserWithdrawalCleanupStatus.CLEANED
						&& lifecycle.getUserId().equals(lineage.getSourceUserId())).isPresent()
				&& sourceRevision != null && !sourceRevision.isActive()
				&& sourceRevision.getRevision() == lineage.getSourceBindingRevision()
				&& targetRevision != null && targetRevision.isActive()
				&& !lineage.getSourceUserId().equals(targetUserId);
		if (!valid) {
			lineage.requireReconciliation("LINEAGE_GATE_MISMATCH", resolvedAt);
			lineageRepository.save(lineage);
			return null;
		}
		OwnerEventCore event = captureService.captureTrialOwnerRebind(
				lineage.getSourceUserId(), targetUserId, consumerScopeId,
				lineage.getSourceBindingRevision(), targetRevision.getRevision(),
				lineage.getLineageId(), resolvedAt);
		lineage.consume(event.getEventId(), targetUserId, targetRevision.getRevision(), resolvedAt);
		lineageRepository.save(lineage);
		return event;
	}
}
