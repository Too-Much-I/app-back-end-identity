package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;

public interface OwnerEventCoreRepository
		extends MongoRepository<OwnerEventCore, String>, OwnerEventCoreRepositoryCustom {

	long deleteByCleanupAtLessThanEqual(Instant cleanupAt);
}
