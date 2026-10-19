package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;

public interface FirebaseEnrollmentAttemptRepository
		extends MongoRepository<FirebaseEnrollmentAttempt, String>,
		FirebaseEnrollmentAttemptRepositoryCustom {

	List<FirebaseEnrollmentAttempt> findAllByFirebaseProjectIdAndFirebaseUidOrderByCreatedAtDesc(
			String firebaseProjectId,
			String firebaseUid
	);
}
