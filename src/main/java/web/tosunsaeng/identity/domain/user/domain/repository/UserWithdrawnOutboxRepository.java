package web.tosunsaeng.identity.domain.user.domain.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawnOutboxStatus;

public interface UserWithdrawnOutboxRepository extends
		MongoRepository<UserWithdrawnOutbox, String>, UserWithdrawnOutboxRepositoryCustom {

	boolean existsByUserId(String userId);

	long countByStatus(UserWithdrawnOutboxStatus status);

	Optional<UserWithdrawnOutbox> findFirstByStatusOrderByWithdrawnAtAsc(
			UserWithdrawnOutboxStatus status
	);

	long deleteByStatusAndCleanupAtLessThanEqual(
			UserWithdrawnOutboxStatus status,
			Instant cleanupAt
	);
}
