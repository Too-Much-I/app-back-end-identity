package web.tosunsaeng.identity.user.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.user.domain.User;

public interface UserRepository extends MongoRepository<User, String> {

	Optional<User> findByNormalizedEmail(String normalizedEmail);

	boolean existsByNormalizedEmail(String normalizedEmail);
}
