package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;

public class FirebaseEnrollmentAttemptRepositoryImpl
		implements FirebaseEnrollmentAttemptRepositoryCustom {

	private final MongoOperations mongoOperations;

	public FirebaseEnrollmentAttemptRepositoryImpl(MongoOperations mongoOperations) {
		this.mongoOperations = Objects.requireNonNull(
				mongoOperations,
				"mongoOperations must not be null"
		);
	}

	@Override
	public Optional<FirebaseEnrollmentAttempt> findPendingByBinding(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		Query query = Query.query(bindingCriteria(
				firebaseProjectId,
				firebaseUid,
				bindingType,
				boundUserId
		).and("status").is(FirebaseEnrollmentStatus.PENDING));
		return Optional.ofNullable(mongoOperations.findOne(
				query,
				FirebaseEnrollmentAttempt.class
		));
	}

	@Override
	public boolean expireIfPendingAndExpired(String enrollmentId, Instant now) {
		Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
		Query query = Query.query(Criteria.where("_id")
				.is(Objects.requireNonNull(enrollmentId, "enrollmentId must not be null"))
				.and("status").is(FirebaseEnrollmentStatus.PENDING)
				.and("expiresAt").lte(requiredNow));
		Update update = new Update()
				.set("status", FirebaseEnrollmentStatus.EXPIRED)
				.unset("consumedAt");
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				FirebaseEnrollmentAttempt.class
		);
		return result.getModifiedCount() == 1;
	}

	@Override
	public boolean consumeIfPendingAndNotExpired(
			String enrollmentId,
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			Instant consumedAt
	) {
		Instant requiredConsumedAt = Objects.requireNonNull(
				consumedAt,
				"consumedAt must not be null"
		);
		Criteria criteria = bindingCriteria(
				firebaseProjectId,
				firebaseUid,
				bindingType,
				boundUserId
		);
		Query query = Query.query(criteria
				.and("_id")
				.is(Objects.requireNonNull(enrollmentId, "enrollmentId must not be null"))
				.and("status").is(FirebaseEnrollmentStatus.PENDING)
				.and("expiresAt").gt(requiredConsumedAt));
		Update update = new Update()
				.set("status", FirebaseEnrollmentStatus.CONSUMED)
				.set("consumedAt", requiredConsumedAt);
		UpdateResult result = mongoOperations.updateFirst(
				query,
				update,
				FirebaseEnrollmentAttempt.class
		);
		return result.getModifiedCount() == 1;
	}

	private static Criteria bindingCriteria(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	) {
		return Criteria.where("firebaseProjectId")
				.is(Objects.requireNonNull(firebaseProjectId, "firebaseProjectId must not be null"))
				.and("firebaseUid")
				.is(Objects.requireNonNull(firebaseUid, "firebaseUid must not be null"))
				.and("bindingType")
				.is(Objects.requireNonNull(bindingType, "bindingType must not be null"))
				.and("boundUserId")
				.is(boundUserId);
	}
}
