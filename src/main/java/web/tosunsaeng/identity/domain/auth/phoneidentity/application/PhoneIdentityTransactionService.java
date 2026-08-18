package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

public class PhoneIdentityTransactionService {

	private final PhoneIdentityRepository phoneIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;
	private final PhoneEligibilityBindingRevisionRepository bindingRevisionRepository;
	private final PhoneEligibilityBindingOutboxRepository bindingOutboxRepository;

	public PhoneIdentityTransactionService(
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository
	) {
		this.phoneIdentityRepository = Objects.requireNonNull(
				phoneIdentityRepository,
				"phoneIdentityRepository must not be null"
		);
		this.aliasRepository = Objects.requireNonNull(
				aliasRepository,
				"aliasRepository must not be null"
		);
		this.bindingRevisionRepository = null;
		this.bindingOutboxRepository = null;
	}

	public PhoneIdentityTransactionService(
			PhoneIdentityRepository phoneIdentityRepository,
			PhoneFingerprintAliasRepository aliasRepository,
			PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			PhoneEligibilityBindingOutboxRepository bindingOutboxRepository
	) {
		this.phoneIdentityRepository = Objects.requireNonNull(phoneIdentityRepository);
		this.aliasRepository = Objects.requireNonNull(aliasRepository);
		this.bindingRevisionRepository = bindingRevisionRepository;
		this.bindingOutboxRepository = bindingOutboxRepository;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public PhoneIdentityLinkResult linkOrReplace(
			String userId,
			PhoneFingerprintSet fingerprints,
			Instant verifiedAt
	) {
		return linkOrReplace(userId, fingerprints, null, null, verifiedAt);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public PhoneIdentityLinkResult linkOrReplace(
			String userId,
			PhoneFingerprintSet fingerprints,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> eligibilityCandidates,
			Instant verifiedAt
	) {
		String requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
		PhoneFingerprintSet requiredFingerprints = Objects.requireNonNull(
				fingerprints,
				"fingerprints must not be null"
		);
		Instant requiredVerifiedAt = Objects.requireNonNull(
				verifiedAt,
				"verifiedAt must not be null"
		);

		List<PhoneFingerprintAlias> matchedAliases = aliasRepository
				.findAllActiveByFingerprints(requiredFingerprints.retained());
		if (matchedAliases.stream().anyMatch(alias -> !alias.getUserId().equals(requiredUserId))) {
			throw new AuthException(AuthErrorStatus.PHONE_ALREADY_LINKED);
		}

		Optional<PhoneIdentity> current = phoneIdentityRepository.findByUserIdAndStatus(
				requiredUserId,
				PhoneIdentityStatus.ACTIVE
		);
		if (!matchedAliases.isEmpty()) {
			PhoneIdentityLinkResult result = reuseOrRotate(
					requiredUserId,
					requiredFingerprints,
					requiredVerifiedAt,
					matchedAliases,
					current
			);
			if (result.outcome() == PhoneIdentityLinkOutcome.ROTATED) {
				publishVerified(requiredUserId, consumerScopeId, eligibilityCandidates, requiredVerifiedAt);
			}
			return result;
		}
		if (current.isPresent()) {
			releaseCurrent(current.orElseThrow(), requiredVerifiedAt);
			publishRevoked(requiredUserId, consumerScopeId, requiredVerifiedAt);
			PhoneIdentityLinkResult result = create(
					requiredUserId,
					requiredFingerprints,
					requiredVerifiedAt,
					PhoneIdentityLinkOutcome.REPLACED
			);
			publishVerified(requiredUserId, consumerScopeId, eligibilityCandidates, requiredVerifiedAt);
			return result;
		}
		PhoneIdentityLinkResult result = create(
				requiredUserId,
				requiredFingerprints,
				requiredVerifiedAt,
				PhoneIdentityLinkOutcome.CREATED
		);
		publishVerified(requiredUserId, consumerScopeId, eligibilityCandidates, requiredVerifiedAt);
		return result;
	}

	private void publishVerified(
			String userId,
			String consumerScopeId,
			List<PhoneEligibilityFingerprintCandidate> candidates,
			Instant verifiedAt
	) {
		if (consumerScopeId == null && candidates == null) return;
		requireBindingRepositories();
		PhoneEligibilityBindingRevision revision = bindingRevisionRepository.advanceVerified(
				userId, Objects.requireNonNull(consumerScopeId), verifiedAt);
		bindingOutboxRepository.save(PhoneEligibilityBindingOutbox.createVerified(
				userId, consumerScopeId, revision.getRevision(),
				Objects.requireNonNull(candidates), verifiedAt, verifiedAt));
	}

	private void publishRevoked(String userId, String consumerScopeId, Instant revokedAt) {
		if (consumerScopeId == null) return;
		requireBindingRepositories();
		bindingRevisionRepository.findByUserIdAndConsumerScopeId(userId, consumerScopeId)
				.filter(PhoneEligibilityBindingRevision::isActive)
				.ifPresent(active -> {
					PhoneEligibilityBindingRevision revoked = bindingRevisionRepository.advanceRevoked(
							userId, consumerScopeId, active.getRevision(), revokedAt)
							.orElseThrow(() -> new IllegalStateException(
									"Eligibility binding revision changed during phone release."));
					bindingOutboxRepository.save(PhoneEligibilityBindingOutbox.createRevoked(
							userId, consumerScopeId, revoked.getRevision(), revokedAt, revokedAt));
				});
	}

	private void requireBindingRepositories() {
		if (bindingRevisionRepository == null || bindingOutboxRepository == null) {
			throw new IllegalStateException("Phone eligibility binding repositories are unavailable.");
		}
	}

	private PhoneIdentityLinkResult reuseOrRotate(
			String userId,
			PhoneFingerprintSet fingerprints,
			Instant verifiedAt,
			List<PhoneFingerprintAlias> matchedAliases,
			Optional<PhoneIdentity> current
	) {
		PhoneIdentity identity = current.orElseThrow(
				() -> new IllegalStateException("Active phone alias has no active identity.")
		);
		boolean inconsistentOwner = matchedAliases.stream()
				.anyMatch(alias -> !alias.getPhoneIdentityId().equals(identity.getPhoneIdentityId()));
		if (inconsistentOwner) {
			throw new IllegalStateException("Active phone aliases have inconsistent ownership.");
		}

		List<PhoneFingerprintAlias> currentAliases = aliasRepository
				.findAllByPhoneIdentityIdAndStatus(
						identity.getPhoneIdentityId(),
						PhoneFingerprintAliasStatus.ACTIVE
				);
		Map<String, PhoneFingerprintAlias> aliasesByVersion = new HashMap<>();
		for (PhoneFingerprintAlias alias : currentAliases) {
			PhoneFingerprintAlias existing = aliasesByVersion.putIfAbsent(
					alias.getFingerprintKeyVersion(),
					alias
			);
			if (existing != null) {
				throw new IllegalStateException("Active phone aliases contain duplicate versions.");
			}
		}

		List<PhoneFingerprintAlias> missingAliases = new ArrayList<>();
		for (PhoneFingerprint fingerprint : fingerprints.retained()) {
			PhoneFingerprintAlias existing = aliasesByVersion.get(fingerprint.keyVersion());
			if (existing == null) {
				missingAliases.add(PhoneFingerprintAlias.create(
						identity.getPhoneIdentityId(),
						userId,
						fingerprint,
						verifiedAt
				));
				continue;
			}
			if (!existing.matches(fingerprint)) {
				throw new IllegalStateException("Phone alias version maps to another fingerprint.");
			}
		}
		if (!missingAliases.isEmpty()) {
			aliasRepository.saveAll(missingAliases);
		}
		boolean currentRotated = identity.rotateCurrentFingerprint(
				fingerprints.activeWrite(),
				verifiedAt
		);
		if (currentRotated) {
			phoneIdentityRepository.save(identity);
		}
		PhoneIdentityLinkOutcome outcome = currentRotated || !missingAliases.isEmpty()
				? PhoneIdentityLinkOutcome.ROTATED
				: PhoneIdentityLinkOutcome.IDEMPOTENT;
		return new PhoneIdentityLinkResult(
				identity.getPhoneIdentityId(),
				userId,
				outcome
		);
	}

	private void releaseCurrent(PhoneIdentity current, Instant releasedAt) {
		long releasedAliases = aliasRepository.releaseAllActiveByPhoneIdentityId(
				current.getPhoneIdentityId(),
				releasedAt
		);
		if (releasedAliases == 0) {
			throw new IllegalStateException("Active phone identity has no active aliases.");
		}
		current.release(releasedAt);
		phoneIdentityRepository.save(current);
	}

	private PhoneIdentityLinkResult create(
			String userId,
			PhoneFingerprintSet fingerprints,
			Instant verifiedAt,
			PhoneIdentityLinkOutcome outcome
	) {
		PhoneIdentity identity = phoneIdentityRepository.save(PhoneIdentity.create(
				userId,
				fingerprints.activeWrite(),
				verifiedAt
		));
		List<PhoneFingerprintAlias> aliases = fingerprints.retained().stream()
				.map(fingerprint -> PhoneFingerprintAlias.create(
						identity.getPhoneIdentityId(),
						userId,
						fingerprint,
						verifiedAt
				))
				.toList();
		aliasRepository.saveAll(aliases);
		return new PhoneIdentityLinkResult(
				identity.getPhoneIdentityId(),
				userId,
				outcome
		);
	}
}
