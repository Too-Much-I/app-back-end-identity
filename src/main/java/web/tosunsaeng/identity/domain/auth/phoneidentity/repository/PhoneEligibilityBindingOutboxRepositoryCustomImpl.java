package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import org.springframework.data.mongodb.core.MongoOperations;

/** Spring Data fragment naming adapter. */
public final class PhoneEligibilityBindingOutboxRepositoryCustomImpl
		extends MongoPhoneEligibilityBindingOutboxOperations {

	public PhoneEligibilityBindingOutboxRepositoryCustomImpl(MongoOperations mongoOperations) {
		super(mongoOperations);
	}
}
