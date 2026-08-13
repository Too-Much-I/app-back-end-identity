package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.time.Instant;
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
}
