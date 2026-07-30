package web.tosunsaeng.identity.domain.auth.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;

public interface RefreshSessionRepository extends MongoRepository<RefreshSession, String> {

	Optional<RefreshSession> findByTokenHash(String tokenHash);

	List<RefreshSession> findAllByUserIdAndRevokedAtIsNull(String userId);

	boolean existsByTokenHash(String tokenHash);
}
