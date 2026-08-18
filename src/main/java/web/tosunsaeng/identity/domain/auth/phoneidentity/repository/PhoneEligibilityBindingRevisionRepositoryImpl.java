package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;

class MongoPhoneEligibilityBindingRevisionOperations
		implements PhoneEligibilityBindingRevisionRepositoryCustom {

	private final MongoOperations mongoOperations;

	MongoPhoneEligibilityBindingRevisionOperations(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(
				mongoOperations,
				"mongoOperations must not be null"
		);
	}

	@Override
	public PhoneEligibilityBindingRevision advanceVerified(
			String userId,
			String consumerScopeId,
			Instant updatedAt
	) {
		String requiredUserId = Objects.requireNonNull(userId, "userId must not be null");
		String requiredScope = Objects.requireNonNull(
				consumerScopeId,
				"consumerScopeId must not be null"
		);
		Instant requiredUpdatedAt = Objects.requireNonNull(
				updatedAt,
				"updatedAt must not be null"
		);
		Query query = Query.query(bindingCriteria(requiredUserId, requiredScope));
		Update update = new Update()
				.setOnInsert("_id", UUID.randomUUID().toString())
				.setOnInsert("userId", requiredUserId)
				.setOnInsert("consumerScopeId", requiredScope)
				.inc("revision", 1L)
				.set("active", true)
				.set("updatedAt", requiredUpdatedAt);
		PhoneEligibilityBindingRevision advanced = mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().upsert(true).returnNew(true),
				PhoneEligibilityBindingRevision.class
		);
		if (advanced == null) {
			throw new IllegalStateException("Eligibility binding revision could not be advanced.");
		}
		if (advanced.getRevision() > PhoneEligibilityBindingOutbox.MAX_BINDING_REVISION) {
			throw new IllegalStateException("Eligibility binding revision is exhausted.");
		}
		return advanced;
	}

	@Override
	public Optional<PhoneEligibilityBindingRevision> advanceRevoked(
			String userId,
			String consumerScopeId,
			long expectedRevision,
			Instant updatedAt
	) {
		if (expectedRevision < 1) {
			throw new IllegalArgumentException("expectedRevision must be positive");
		}
		Query query = Query.query(bindingCriteria(
				Objects.requireNonNull(userId, "userId must not be null"),
				Objects.requireNonNull(consumerScopeId, "consumerScopeId must not be null")
		).and("revision").is(expectedRevision).and("active").is(true));
		Update update = new Update()
				.inc("revision", 1L)
				.set("active", false)
				.set("updatedAt", Objects.requireNonNull(updatedAt, "updatedAt must not be null"));
		PhoneEligibilityBindingRevision advanced = mongoOperations.findAndModify(
				query,
				update,
				FindAndModifyOptions.options().returnNew(true),
				PhoneEligibilityBindingRevision.class
		);
		if (advanced != null
				&& advanced.getRevision() > PhoneEligibilityBindingOutbox.MAX_BINDING_REVISION) {
			throw new IllegalStateException("Eligibility binding revision is exhausted.");
		}
		return Optional.ofNullable(advanced);
	}

	private static Criteria bindingCriteria(String userId, String consumerScopeId) {
		return Criteria.where("userId").is(userId)
				.and("consumerScopeId").is(consumerScopeId);
	}
}
