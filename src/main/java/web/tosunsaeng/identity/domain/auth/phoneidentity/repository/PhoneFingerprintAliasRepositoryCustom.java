package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;

public interface PhoneFingerprintAliasRepositoryCustom {

	List<PhoneFingerprintAlias> findAllActiveByFingerprints(
			Collection<PhoneFingerprint> fingerprints
	);

	long releaseAllActiveByPhoneIdentityId(String phoneIdentityId, Instant releasedAt);
}
