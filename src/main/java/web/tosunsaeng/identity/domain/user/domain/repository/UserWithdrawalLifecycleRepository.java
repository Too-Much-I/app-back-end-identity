package web.tosunsaeng.identity.domain.user.domain.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;

public interface UserWithdrawalLifecycleRepository
		extends MongoRepository<UserWithdrawalLifecycle, String> {

	Optional<UserWithdrawalLifecycle> findByUserId(String userId);
}
