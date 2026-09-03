package web.tosunsaeng.identity.domain.user.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class UserWithdrawalIdentityReleaseTransactionService {

	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final UserRepository userRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final PhoneIdentityRepository phoneIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneEligibilityBindingRevisionRepository bindingRevisionRepository;
	private final PhoneEligibilityBindingOutboxRepository bindingOutboxRepository;
	private final PhoneRejoinLineageRepository lineageRepository;
	private final OwnerEventProperties ownerEventProperties;

	public UserWithdrawalIdentityReleaseTransactionService(
			UserWithdrawalLifecycleRepository lifecycleRepository,
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			PhoneEligibilityBindingOutboxRepository bindingOutboxRepository
	) {
		this(lifecycleRepository, userRepository, firebaseIdentityRepository,
				socialIdentityRepository, phoneIdentityRepository, aliasRepository,
				bindingRevisionRepository, bindingOutboxRepository, null, null);
	}

	public UserWithdrawalIdentityReleaseTransactionService(
			UserWithdrawalLifecycleRepository lifecycleRepository,
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			PhoneEligibilityBindingOutboxRepository bindingOutboxRepository,
			PhoneRejoinLineageRepository lineageRepository,
			OwnerEventProperties ownerEventProperties
	) {
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.phoneIdentityRepository = Objects.requireNonNull(phoneIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.bindingRevisionRepository = Objects.requireNonNull(bindingRevisionRepository);
		this.bindingOutboxRepository = Objects.requireNonNull(bindingOutboxRepository);
		this.lineageRepository = lineageRepository;
		this.ownerEventProperties = ownerEventProperties;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public IdentityReleaseOutcome release(
			UserWithdrawalLifecycle selected,
			Instant releasedAt
	) {
		UserWithdrawalLifecycle requiredSelected = Objects.requireNonNull(
				selected,
				"selected must not be null"
		);
		Instant requiredReleasedAt = Objects.requireNonNull(
				releasedAt,
				"releasedAt must not be null"
		);
		UserWithdrawalLifecycle lifecycle = lifecycleRepository.findById(
				requiredSelected.getWithdrawalId()
		).orElseThrow(() -> concurrentChange("Withdrawal lifecycle disappeared."));
		if (lifecycle.getStatus() == UserWithdrawalCleanupStatus.CLEANED) {
			return IdentityReleaseOutcome.IDEMPOTENT;
		}
		if (lifecycle.getStatus() != UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
				|| !Objects.equals(lifecycle.getVersion(), requiredSelected.getVersion())) {
			throw concurrentChange("Withdrawal lifecycle changed before identity release.");
		}
		long expectedVersion = requireVersion(lifecycle);

		WithdrawalCleanupFailureCode lifecycleFailure = validateLifecycle(lifecycle);
		if (lifecycleFailure != null) {
			return reconcile(lifecycle, expectedVersion, lifecycleFailure, requiredReleasedAt);
		}
		Optional<User> user = userRepository.findById(lifecycle.getUserId());
		if (user.isEmpty() || user.orElseThrow().getStatus() != UserStatus.WITHDRAWN) {
			return reconcile(
					lifecycle,
					expectedVersion,
					WithdrawalCleanupFailureCode.IDENTITY_RELEASE_OWNERSHIP_MISMATCH,
					requiredReleasedAt
			);
		}

		FirebaseResolution firebaseResolution = resolveFirebaseIdentity(lifecycle);
		if (firebaseResolution.failureCode() != null) {
			return reconcile(
					lifecycle,
					expectedVersion,
					firebaseResolution.failureCode(),
					requiredReleasedAt
			);
		}
		List<SocialIdentity> socialIdentities = List.copyOf(
				socialIdentityRepository.findAllByUserId(lifecycle.getUserId())
		);
		Optional<PhoneIdentity> phoneIdentity = phoneIdentityRepository.findByUserIdAndStatus(
				lifecycle.getUserId(),
				PhoneIdentityStatus.ACTIVE
		);
		List<PhoneFingerprintAlias> activeAliases = List.copyOf(
				aliasRepository.findAllByUserIdAndStatus(
						lifecycle.getUserId(),
						PhoneFingerprintAliasStatus.ACTIVE
				)
		);
		List<PhoneFingerprintAlias> identityAliases = phoneIdentity
				.map(identity -> List.copyOf(aliasRepository.findAllByPhoneIdentityIdAndStatus(
						identity.getPhoneIdentityId(),
						PhoneFingerprintAliasStatus.ACTIVE
				)))
				.orElseGet(List::of);
		if (!phoneStateConsistent(
				lifecycle.getUserId(), phoneIdentity, activeAliases, identityAliases
		)) {
			return reconcile(
					lifecycle,
					expectedVersion,
					WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
					requiredReleasedAt
			);
		}
		List<PhoneEligibilityBindingRevision> activeBindings = List.copyOf(
				bindingRevisionRepository.findAllByUserIdAndActiveTrue(lifecycle.getUserId())
		);
		List<PhoneEligibilityBindingRevision> bindingHistory = List.copyOf(
				bindingRevisionRepository.findAllByUserId(lifecycle.getUserId())
		);
		boolean allIdentitiesAlreadyReleased = firebaseResolution.identity() == null
				&& socialIdentities.isEmpty()
				&& phoneIdentity.isEmpty()
				&& activeAliases.isEmpty();
		if (lifecycle.getFirebaseProjectId() != null
				&& firebaseResolution.identity() == null
				&& !allIdentitiesAlreadyReleased) {
			return reconcile(
					lifecycle,
					expectedVersion,
					WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
					requiredReleasedAt
			);
		}
		if (allIdentitiesAlreadyReleased && !activeBindings.isEmpty()) {
			return reconcile(
					lifecycle,
					expectedVersion,
					WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE,
					requiredReleasedAt
			);
		}

		List<PhoneEligibilityBindingRevision> revokedBindings = revokeActiveBindings(
				activeBindings, requiredReleasedAt);
		if (revokedBindings.isEmpty() && phoneIdentity.isPresent()) {
			revokedBindings = bindingHistory.stream()
					.filter(binding -> !binding.isActive())
					.toList();
		}
		if (phoneIdentity.isPresent() && lineageRepository != null && ownerEventProperties != null
				&& ownerEventProperties.isTrialRebindCaptureEnabled()) {
			PhoneIdentity sourcePhone = phoneIdentity.orElseThrow();
			List<PhoneRejoinLineage> lineages = revokedBindings.stream()
					.map(revoked -> PhoneRejoinLineage.available(
							lifecycle.getWithdrawalId(), lifecycle.getUserId(),
							sourcePhone.getPhoneIdentityId(), revoked.getConsumerScopeId(),
							revoked.getRevision(), requiredReleasedAt))
					.toList();
			if (!lineages.isEmpty()) lineageRepository.saveAll(lineages);
		}
		if (firebaseResolution.identity() != null) {
			firebaseIdentityRepository.deleteById(
					firebaseResolution.identity().getFirebaseIdentityId()
			);
		}
		if (!socialIdentities.isEmpty()) {
			socialIdentityRepository.deleteAll(socialIdentities);
		}
		if (phoneIdentity.isPresent()) {
			PhoneIdentity identity = phoneIdentity.orElseThrow();
			long releasedAliases = aliasRepository.releaseAllActiveByPhoneIdentityId(
					identity.getPhoneIdentityId(),
					requiredReleasedAt
			);
			if (releasedAliases != identityAliases.size()) {
				throw concurrentChange("Phone aliases changed during identity release.");
			}
			identity.release(requiredReleasedAt);
			phoneIdentityRepository.save(identity);
		}
		if (!lifecycleRepository.markIdentityReleaseCleaned(
				lifecycle.getWithdrawalId(),
				expectedVersion,
				requiredReleasedAt
		)) {
			throw concurrentChange("Withdrawal lifecycle CLEANED claim was lost.");
		}
		return allIdentitiesAlreadyReleased
				? IdentityReleaseOutcome.IDEMPOTENT
				: IdentityReleaseOutcome.CLEANED;
	}

	private WithdrawalCleanupFailureCode validateLifecycle(
			UserWithdrawalLifecycle lifecycle
	) {
		if (lifecycle.getExternalDeletedAt() == null
				|| lifecycle.getExternalDeletedAt().isBefore(lifecycle.getRequestedAt())
				|| lifecycle.getLeaseOwner() != null
				|| lifecycle.getLeaseUntil() != null) {
			return WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PRECONDITION_FAILED;
		}
		boolean projectMissing = lifecycle.getFirebaseProjectId() == null;
		boolean uidMissing = lifecycle.getFirebaseUid() == null;
		return projectMissing == uidMissing
				? null
				: WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PRECONDITION_FAILED;
	}

	private FirebaseResolution resolveFirebaseIdentity(
			UserWithdrawalLifecycle lifecycle
	) {
		Optional<FirebaseIdentity> byUser = firebaseIdentityRepository.findByUserId(
				lifecycle.getUserId()
		);
		if (lifecycle.getFirebaseProjectId() == null) {
			return byUser.isEmpty()
					? FirebaseResolution.empty()
					: FirebaseResolution.failure(
							WithdrawalCleanupFailureCode.IDENTITY_RELEASE_OWNERSHIP_MISMATCH
					);
		}
		Optional<FirebaseIdentity> byTarget = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						lifecycle.getFirebaseProjectId(),
						lifecycle.getFirebaseUid()
				);
		if (byUser.isEmpty() && byTarget.isEmpty()) {
			return FirebaseResolution.empty();
		}
		if (byUser.isEmpty() || byTarget.isEmpty()) {
			return FirebaseResolution.failure(
					WithdrawalCleanupFailureCode.IDENTITY_RELEASE_PARTIAL_STATE
			);
		}
		FirebaseIdentity userIdentity = byUser.orElseThrow();
		FirebaseIdentity targetIdentity = byTarget.orElseThrow();
		boolean exact = userIdentity.getFirebaseIdentityId().equals(
				targetIdentity.getFirebaseIdentityId()
		) && lifecycle.getUserId().equals(userIdentity.getUserId())
				&& lifecycle.getFirebaseProjectId().equals(userIdentity.getFirebaseProjectId())
				&& lifecycle.getFirebaseUid().equals(userIdentity.getFirebaseUid());
		return exact
				? FirebaseResolution.resolved(userIdentity)
				: FirebaseResolution.failure(
						WithdrawalCleanupFailureCode.IDENTITY_RELEASE_OWNERSHIP_MISMATCH
				);
	}

	private boolean phoneStateConsistent(
			String userId,
			Optional<PhoneIdentity> phoneIdentity,
			List<PhoneFingerprintAlias> userAliases,
			List<PhoneFingerprintAlias> identityAliases
	) {
		if (phoneIdentity.isEmpty()) {
			return userAliases.isEmpty() && identityAliases.isEmpty();
		}
		PhoneIdentity identity = phoneIdentity.orElseThrow();
		if (!userId.equals(identity.getUserId())
				|| userAliases.isEmpty()
				|| userAliases.size() != identityAliases.size()) {
			return false;
		}
		return userAliases.stream().allMatch(alias -> userId.equals(alias.getUserId())
				&& identity.getPhoneIdentityId().equals(alias.getPhoneIdentityId())
				&& identityAliases.stream().anyMatch(identityAlias ->
						identityAlias.getAliasId().equals(alias.getAliasId())))
				&& identityAliases.stream().allMatch(alias -> userId.equals(alias.getUserId())
				&& identity.getPhoneIdentityId().equals(alias.getPhoneIdentityId()));
	}

	private List<PhoneEligibilityBindingRevision> revokeActiveBindings(
			List<PhoneEligibilityBindingRevision> activeBindings,
			Instant revokedAt
	) {
		java.util.ArrayList<PhoneEligibilityBindingRevision> revokedBindings = new java.util.ArrayList<>();
		for (PhoneEligibilityBindingRevision active : activeBindings) {
			PhoneEligibilityBindingRevision revoked = bindingRevisionRepository.advanceRevoked(
					active.getUserId(),
					active.getConsumerScopeId(),
					active.getRevision(),
					revokedAt
			).orElseThrow(() -> concurrentChange(
					"Eligibility binding changed during identity release."
			));
			bindingOutboxRepository.save(PhoneEligibilityBindingOutbox.createRevoked(
					revoked.getUserId(),
					revoked.getConsumerScopeId(),
					revoked.getRevision(),
					revokedAt,
					revokedAt
			));
			revokedBindings.add(revoked);
		}
		return List.copyOf(revokedBindings);
	}

	private IdentityReleaseOutcome reconcile(
			UserWithdrawalLifecycle lifecycle,
			long expectedVersion,
			WithdrawalCleanupFailureCode failureCode,
			Instant updatedAt
	) {
		if (!lifecycleRepository.markIdentityReleaseReconciliationRequired(
				lifecycle.getWithdrawalId(),
				expectedVersion,
				failureCode,
				updatedAt
		)) {
			throw concurrentChange("Withdrawal lifecycle reconciliation claim was lost.");
		}
		return IdentityReleaseOutcome.RECONCILIATION_REQUIRED;
	}

	private static long requireVersion(UserWithdrawalLifecycle lifecycle) {
		Long version = lifecycle.getVersion();
		if (version == null || version < 0) {
			throw new IllegalStateException("Identity release lifecycle must have a version.");
		}
		return version;
	}

	private static OptimisticLockingFailureException concurrentChange(String message) {
		return new OptimisticLockingFailureException(message);
	}

	private record FirebaseResolution(
			FirebaseIdentity identity,
			WithdrawalCleanupFailureCode failureCode
	) {
		private FirebaseResolution {
			if (identity != null && failureCode != null) {
				throw new IllegalArgumentException("Resolved identity cannot have a failure code.");
			}
		}

		static FirebaseResolution empty() {
			return new FirebaseResolution(null, null);
		}

		static FirebaseResolution resolved(FirebaseIdentity identity) {
			return new FirebaseResolution(Objects.requireNonNull(identity), null);
		}

		static FirebaseResolution failure(WithdrawalCleanupFailureCode code) {
			return new FirebaseResolution(null, Objects.requireNonNull(code));
		}
	}
}
