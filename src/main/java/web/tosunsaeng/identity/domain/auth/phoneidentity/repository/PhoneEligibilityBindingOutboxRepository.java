package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;

public interface PhoneEligibilityBindingOutboxRepository
		extends MongoRepository<PhoneEligibilityBindingOutbox, String>,
		PhoneEligibilityBindingOutboxRepositoryCustom {

	long deleteByStatusAndCleanupAtLessThanEqual(
			PhoneEligibilityBindingOutboxStatus status,
			Instant cleanupAt
	);
}
