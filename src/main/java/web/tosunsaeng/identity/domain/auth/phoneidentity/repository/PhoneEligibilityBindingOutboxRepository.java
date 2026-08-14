package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;

public interface PhoneEligibilityBindingOutboxRepository
		extends MongoRepository<PhoneEligibilityBindingOutbox, String> {
}
