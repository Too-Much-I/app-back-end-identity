package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.domain.Sort;
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

	@Override
	public long scheduleCleanupForTarget(
			String firebaseProjectId,
			String firebaseUid,
			Instant cleanupAt
	) {
		Instant requiredCleanupAt = Objects.requireNonNull(
				cleanupAt,
				"cleanupAt must not be null"
		);
		Query query = Query.query(Criteria.where("firebaseProjectId")
				.is(Objects.requireNonNull(firebaseProjectId))
				.and("firebaseUid").is(Objects.requireNonNull(firebaseUid)));
		UpdateResult result = mongoOperations.updateMulti(
				query,
				new Update().set("cleanupAt", requiredCleanupAt),
				FirebaseEnrollmentAttempt.class
		);
		return result.getModifiedCount();
	}

	@Override
	public long clearCleanupAtForTarget(String firebaseProjectId, String firebaseUid) {
		Query query = Query.query(Criteria.where("firebaseProjectId")
				.is(Objects.requireNonNull(firebaseProjectId))
				.and("firebaseUid").is(Objects.requireNonNull(firebaseUid))
				.and("cleanupAt").exists(true));
		UpdateResult result = mongoOperations.updateMulti(
				query,
				new Update().unset("cleanupAt"),
				FirebaseEnrollmentAttempt.class
		);
		return result.getModifiedCount();
	}

	@Override
	public List<FirebaseEnrollmentAttempt> findLegacyCaptureCandidates(
			Instant lowerBound,
			Instant upperBound,
			Instant now,
			int limit
	) {
		Instant requiredLower = Objects.requireNonNull(lowerBound);
		Instant requiredUpper = Objects.requireNonNull(upperBound);
		Instant requiredNow = Objects.requireNonNull(now);
		if (!requiredUpper.isAfter(requiredLower)) {
			throw new IllegalArgumentException("upperBound must be after lowerBound");
		}
		if (limit < 1 || limit > 100) {
			throw new IllegalArgumentException("limit must be between 1 and 100");
		}
		Query query = Query.query(new Criteria().andOperator(
				Criteria.where("createdAt").gte(requiredLower).lt(requiredUpper),
				Criteria.where("expiresAt").lte(requiredNow),
				Criteria.where("status").in(
						FirebaseEnrollmentStatus.PENDING, FirebaseEnrollmentStatus.EXPIRED
				)
		)).with(Sort.by(Sort.Direction.DESC, "createdAt", "_id")).limit(limit);
		return mongoOperations.find(query, FirebaseEnrollmentAttempt.class);
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
