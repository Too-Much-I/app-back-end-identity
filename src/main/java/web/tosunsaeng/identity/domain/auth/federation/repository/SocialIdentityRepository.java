package web.tosunsaeng.identity.domain.auth.federation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

public interface SocialIdentityRepository extends MongoRepository<SocialIdentity, String> {

	Optional<SocialIdentity> findByProviderAndProviderSubject(
			SocialProvider provider,
			String providerSubject
	);

	List<SocialIdentity> findAllByUserId(String userId);
}
