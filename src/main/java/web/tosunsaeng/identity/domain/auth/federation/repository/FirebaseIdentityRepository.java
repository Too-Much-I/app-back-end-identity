package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;

public interface FirebaseIdentityRepository extends MongoRepository<FirebaseIdentity, String> {

	Optional<FirebaseIdentity> findByFirebaseProjectIdAndFirebaseUid(
			String firebaseProjectId,
			String firebaseUid
	);

	Optional<FirebaseIdentity> findByUserId(String userId);
}
