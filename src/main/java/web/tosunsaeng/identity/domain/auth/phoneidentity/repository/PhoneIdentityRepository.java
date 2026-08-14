package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;

public interface PhoneIdentityRepository extends MongoRepository<PhoneIdentity, String> {

	Optional<PhoneIdentity> findByUserIdAndStatus(String userId, PhoneIdentityStatus status);
}
