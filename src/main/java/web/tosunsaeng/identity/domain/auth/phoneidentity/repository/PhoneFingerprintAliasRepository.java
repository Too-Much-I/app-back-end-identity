package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

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

	List<PhoneFingerprintAlias> findAllByUserIdAndStatus(
			String userId,
			PhoneFingerprintAliasStatus status
	);
}
