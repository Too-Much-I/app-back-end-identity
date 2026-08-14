package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;

public interface PhoneFingerprintAliasRepository extends
		MongoRepository<PhoneFingerprintAlias, String>,
		PhoneFingerprintAliasRepositoryCustom {

	List<PhoneFingerprintAlias> findAllByPhoneIdentityIdAndStatus(
			String phoneIdentityId,
			PhoneFingerprintAliasStatus status
	);
}
