package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;

public interface PhoneEligibilityBindingRevisionRepository
		extends MongoRepository<PhoneEligibilityBindingRevision, String>,
		PhoneEligibilityBindingRevisionRepositoryCustom {

	List<PhoneEligibilityBindingRevision> findAllByUserIdAndActiveTrue(String userId);

	Optional<PhoneEligibilityBindingRevision> findByUserIdAndConsumerScopeId(
			String userId,
			String consumerScopeId
	);
}
