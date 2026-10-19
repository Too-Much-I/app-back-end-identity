package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;

public interface PhoneFingerprintAliasRepositoryCustom {

	List<PhoneFingerprintAlias> findAllActiveByFingerprints(
			Collection<PhoneFingerprint> fingerprints
	);

	List<PhoneFingerprintAlias> findAllByFingerprintsAndStatus(
			Collection<PhoneFingerprint> fingerprints,
			PhoneFingerprintAliasStatus status
	);

	long releaseAllActiveByPhoneIdentityId(String phoneIdentityId, Instant releasedAt);
}
