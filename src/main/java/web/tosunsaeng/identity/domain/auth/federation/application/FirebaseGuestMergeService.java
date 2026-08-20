package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

public final class FirebaseGuestMergeService implements FirebaseGuestMergeUseCase {

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseGuestMergeTargetResolver targetResolver;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseGuestMergeTransactionService transactionService;
	private final AccessTokenIssuer accessTokenIssuer;
	private final Clock clock;

	public FirebaseGuestMergeService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseGuestMergeTargetResolver targetResolver,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseGuestMergeTransactionService transactionService,
			AccessTokenIssuer accessTokenIssuer,
			Clock clock
	) {
		this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.authenticationVerifier = Objects.requireNonNull(authenticationVerifier);
		this.targetResolver = Objects.requireNonNull(targetResolver);
		this.refreshSessionIssuer = Objects.requireNonNull(refreshSessionIssuer);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public FirebaseSignupResponse merge(FirebaseGuestMergeRequest request) {
		FirebaseGuestMergeRequest requiredRequest = Objects.requireNonNull(request);
		String sourceUserId = currentUserProvider.getCurrentUserId();
		User source = userRepository.findById(sourceUserId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (source.getStatus() != UserStatus.ACTIVE || !source.isGuest()) {
			throw new AuthException(AuthErrorStatus.GUEST_MERGE_NOT_ALLOWED);
		}
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.GUEST_MERGE
		);
		User target = targetResolver.resolve(principal, sourceUserId);
		Instant now = clock.instant();
		Instant expectedSourceUpdatedAt = source.getUpdatedAt();
		User mergedSource = source.toMergedTombstone(target.getUserId(), now);
		PreparedRefreshSession preparedTargetSession = refreshSessionIssuer.prepare(
				target.getUserId()
		);
		UserMergedOutbox outbox = UserMergedOutbox.create(
				sourceUserId,
				target.getUserId(),
				now
		);
		IssuedRefreshSession refreshSession = transactionService.merge(
				mergedSource,
				expectedSourceUpdatedAt,
				preparedTargetSession,
				outbox,
				now
		);
		IssuedAccessToken accessToken = accessTokenIssuer.issue(target.getUserId(), Set.of());
		return new FirebaseSignupResponse(
				accessToken.tokenValue(),
				refreshSession.tokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshSession.issuedAt(), refreshSession.expiresAt()).toMillis()
		);
	}
}
