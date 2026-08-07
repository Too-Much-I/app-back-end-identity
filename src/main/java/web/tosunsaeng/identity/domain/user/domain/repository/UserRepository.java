package web.tosunsaeng.identity.domain.user.domain.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.user.domain.entity.User;

public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

	Optional<User> findByNormalizedEmail(String normalizedEmail);

	boolean existsByNormalizedEmail(String normalizedEmail);

	boolean existsByGuestInstallationIdHash(String guestInstallationIdHash);
}
