package web.tosunsaeng.identity.domain.auth.application;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;

/** Temporary installation-ID authentication; remove/disable after the SNS transition. */
@Service
public class GuestRecoveryTransactionService {

	private final MongoTemplate mongoTemplate;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter responseConverter;
	private final boolean enabled;

	public GuestRecoveryTransactionService(
			MongoTemplate mongoTemplate,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			AuthResponseConverter responseConverter,
			@Value("${app.guest-recovery.enabled:false}") boolean enabled
	) {
		this.mongoTemplate = mongoTemplate;
		this.accessTokenIssuer = accessTokenIssuer;
		this.refreshSessionIssuer = refreshSessionIssuer;
		this.responseConverter = responseConverter;
		this.enabled = enabled;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public GuestAuthResponse recover(String installationIdHash) {
		if (!enabled || installationIdHash == null || installationIdHash.isBlank()) {
			throw new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		}
		// A real write (not a no-op) conflicts with concurrent withdrawal/status writes.
		// It is rolled back with session insertion and does not overwrite profile/consents.
		User guest = mongoTemplate.findAndModify(
				Query.query(Criteria.where("guestInstallationIdHash").is(installationIdHash)
						.and("provider").is(UserProvider.GUEST)
						.and("status").is(UserStatus.ACTIVE)),
				new Update().inc("guestRecoveryFence", 1L),
				FindAndModifyOptions.options().returnNew(true),
				User.class
		);
		if (guest == null) {
			throw new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		}
		var access = accessTokenIssuer.issue(guest.getUserId(), Set.of());
		var refresh = refreshSessionIssuer.issue(guest.getUserId());
		return responseConverter.toGuestAuthResponse(
				access, refresh.tokenValue(), refresh.issuedAt(), refresh.expiresAt()
		);
	}
}
