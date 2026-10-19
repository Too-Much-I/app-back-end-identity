package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.List;
import java.util.Objects;

import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;

public class FirebaseAuthMethodsSyncTransactionService {

	private final SocialIdentityRepository socialIdentityRepository;

	public FirebaseAuthMethodsSyncTransactionService(
			SocialIdentityRepository socialIdentityRepository
	) {
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public void saveMissing(List<SocialIdentity> socialIdentities) {
		List<SocialIdentity> requiredIdentities = List.copyOf(
				Objects.requireNonNull(socialIdentities)
		);
		if (!requiredIdentities.isEmpty()) {
			throw new web.tosunsaeng.identity.domain.auth.common.exception.AuthException(
					web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus.PROVIDER_LINK_RETIRED);
		}
	}
}
