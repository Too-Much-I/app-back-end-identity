package web.tosunsaeng.identity.domain.auth.usermerge.repository;

import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.UserMergedOutboxStatus;

public interface UserMergedOutboxRepository
		extends MongoRepository<UserMergedOutbox, String>, UserMergedOutboxRepositoryCustom {

	long deleteByStatusAndCleanupAtLessThanEqual(
			UserMergedOutboxStatus status,
			Instant cleanupAt
	);
}
