package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.phone.PhoneFingerprint;

public interface PhoneFingerprintAliasRepositoryCustom {

	List<PhoneFingerprintAlias> findAllActiveByFingerprints(
			Collection<PhoneFingerprint> fingerprints
	);

	long releaseAllActiveByPhoneIdentityId(String phoneIdentityId, Instant releasedAt);
}
