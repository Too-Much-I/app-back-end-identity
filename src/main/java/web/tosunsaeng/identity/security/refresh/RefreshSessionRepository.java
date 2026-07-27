package web.tosunsaeng.identity.security.refresh;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RefreshSessionRepository extends MongoRepository<RefreshSession, String> {

	Optional<RefreshSession> findByTokenHash(String tokenHash);

	List<RefreshSession> findAllByUserIdAndRevokedAtIsNull(String userId);

	boolean existsByTokenHash(String tokenHash);
}
