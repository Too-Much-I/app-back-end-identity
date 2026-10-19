package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingDeliveryScopeState;

public interface PhoneEligibilityBindingDeliveryScopeStateRepository
		extends MongoRepository<PhoneEligibilityBindingDeliveryScopeState, String> {
}
