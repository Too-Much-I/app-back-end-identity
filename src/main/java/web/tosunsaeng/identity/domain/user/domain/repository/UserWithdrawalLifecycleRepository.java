package web.tosunsaeng.identity.domain.user.domain.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;

public interface UserWithdrawalLifecycleRepository
		extends MongoRepository<UserWithdrawalLifecycle, String>,
		UserWithdrawalLifecycleRepositoryCustom {

	Optional<UserWithdrawalLifecycle> findByUserId(String userId);

	Optional<UserWithdrawalLifecycle> findFirstByStatusOrderByExternalDeletedAtAscWithdrawalIdAsc(
			UserWithdrawalCleanupStatus status
	);
}
