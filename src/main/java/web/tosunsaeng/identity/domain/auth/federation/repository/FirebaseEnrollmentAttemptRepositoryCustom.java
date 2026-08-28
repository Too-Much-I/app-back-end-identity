package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;

public interface FirebaseEnrollmentAttemptRepositoryCustom {

	Optional<FirebaseEnrollmentAttempt> findPendingByBinding(
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId
	);

	boolean expireIfPendingAndExpired(String enrollmentId, Instant now);

	boolean consumeIfPendingAndNotExpired(
			String enrollmentId,
			String firebaseProjectId,
			String firebaseUid,
			FirebaseEnrollmentBindingType bindingType,
			String boundUserId,
			Instant consumedAt
	);

	long scheduleCleanupForTarget(
			String firebaseProjectId,
			String firebaseUid,
			Instant cleanupAt
	);

	long clearCleanupAtForTarget(String firebaseProjectId, String firebaseUid);

	List<FirebaseEnrollmentAttempt> findLegacyCaptureCandidates(
			Instant lowerBound,
			Instant upperBound,
			Instant now,
			int limit
	);
}
