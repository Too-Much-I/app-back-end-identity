package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;

public interface PhoneEligibilityBindingRevisionRepositoryCustom {

	PhoneEligibilityBindingRevision advanceVerified(
			String userId,
			String consumerScopeId,
			Instant updatedAt
	);

	Optional<PhoneEligibilityBindingRevision> advanceRevoked(
			String userId,
			String consumerScopeId,
			long expectedRevision,
			Instant updatedAt
	);
}
