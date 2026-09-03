package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneRejoinLineageStatus;

public interface PhoneRejoinLineageRepository extends MongoRepository<PhoneRejoinLineage, String> {
	Optional<PhoneRejoinLineage> findByClaimedEventId(String eventId);
	List<PhoneRejoinLineage> findAllBySourcePhoneIdentityIdInAndConsumerScopeIdAndStatus(
			Collection<String> phoneIdentityIds,
			String consumerScopeId,
			PhoneRejoinLineageStatus status);
}
