package web.tosunsaeng.identity.domain.auth.application.phone;

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
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.domain.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.domain.repository.PhoneIdentityRepository;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;

public class PhoneIdentityTransactionService {

	private final PhoneIdentityRepository phoneIdentityRepository;
	private final PhoneFingerprintAliasRepository aliasRepository;

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
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public PhoneIdentityLinkResult linkOrReplace(
			String userId,
			PhoneFingerprintSet fingerprints,
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
			return reuseOrRotate(
					requiredUserId,
					requiredFingerprints,
					requiredVerifiedAt,
					matchedAliases,
					current
			);
		}
		if (current.isPresent()) {
			releaseCurrent(current.orElseThrow(), requiredVerifiedAt);
			return create(
					requiredUserId,
					requiredFingerprints,
					requiredVerifiedAt,
					PhoneIdentityLinkOutcome.REPLACED
			);
		}
		return create(
				requiredUserId,
				requiredFingerprints,
				requiredVerifiedAt,
				PhoneIdentityLinkOutcome.CREATED
		);
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
