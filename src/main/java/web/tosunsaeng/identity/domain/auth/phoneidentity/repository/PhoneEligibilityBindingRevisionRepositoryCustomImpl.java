package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import org.springframework.data.mongodb.core.MongoOperations;

/** Spring Data fragment naming adapter. */
public final class PhoneEligibilityBindingRevisionRepositoryCustomImpl
		extends MongoPhoneEligibilityBindingRevisionOperations {

	public PhoneEligibilityBindingRevisionRepositoryCustomImpl(MongoOperations mongoOperations) {
		super(mongoOperations);
	}
}
