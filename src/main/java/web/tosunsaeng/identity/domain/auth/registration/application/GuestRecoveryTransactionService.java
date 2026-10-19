package web.tosunsaeng.identity.domain.auth.registration.application;

import java.util.Set;

import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.application.SessionAuthentication;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
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
						.and("status").is(UserStatus.ACTIVE)
						.and("mergedIntoUserId").is(null).and("mergedAt").is(null)
						.orOperator(Criteria.where("accountType").is(UserAccountType.GUEST),
								Criteria.where("accountType").is(null))),
				new Update().inc("guestRecoveryFence", 1L),
				FindAndModifyOptions.options().returnNew(true),
				User.class
		);
		if (guest == null || guest.getStatus() != UserStatus.ACTIVE
				|| guest.getProvider() != UserProvider.GUEST || !guest.isGuest()
				|| guest.getMergedIntoUserId() != null || guest.getMergedAt() != null
				|| !installationIdHash.equals(guest.getGuestInstallationIdHash())) {
			throw new AuthException(AuthErrorStatus.GUEST_ALREADY_EXISTS);
		}
		// Capture the current security epoch; never revive a revoked Refresh Session.
		long epoch = refreshSessionIssuer.captureEpoch(guest.getUserId());
		var access = accessTokenIssuer.issue(guest.getUserId(), UserAccountType.GUEST, Set.of());
		var refresh = refreshSessionIssuer.isFenceEnabled()
				? refreshSessionIssuer.issueAuthenticated(guest.getUserId(), new SessionAuthentication(
						epoch, SessionAuthentication.Source.GUEST, null, null))
				: refreshSessionIssuer.issue(guest.getUserId());
		return responseConverter.toGuestAuthResponse(
				access, refresh.tokenValue(), refresh.issuedAt(), refresh.expiresAt()
		);
	}
}
