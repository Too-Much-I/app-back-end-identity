package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.AbandonedFirebaseEnrollmentCleanup;

public interface AbandonedFirebaseEnrollmentCleanupRepository extends
		MongoRepository<AbandonedFirebaseEnrollmentCleanup, String>,
		AbandonedFirebaseEnrollmentCleanupRepositoryCustom {

	Optional<AbandonedFirebaseEnrollmentCleanup> findByFirebaseProjectIdAndFirebaseUid(
			String firebaseProjectId,
			String firebaseUid
	);
}
