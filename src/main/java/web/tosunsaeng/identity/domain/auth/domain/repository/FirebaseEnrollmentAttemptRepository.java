package web.tosunsaeng.identity.domain.auth.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;

public interface FirebaseEnrollmentAttemptRepository
		extends MongoRepository<FirebaseEnrollmentAttempt, String>,
		FirebaseEnrollmentAttemptRepositoryCustom {
}
