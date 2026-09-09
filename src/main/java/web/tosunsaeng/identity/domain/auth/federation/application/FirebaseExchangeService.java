package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthenticatedResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequiredResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequirement;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseExchangeResponse;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

public final class FirebaseExchangeService implements FirebaseExchangeUseCase {

	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final UserRepository userRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final FirebaseEnrollmentAttemptService enrollmentAttemptService;
	private final Clock clock;
	private final UserWithdrawalLifecycleRepository withdrawalLifecycleRepository;
	private final WithdrawalEnrollmentGate withdrawalEnrollmentGate;

	public FirebaseExchangeService(
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptService enrollmentAttemptService,
			Clock clock
	) {
		this(authenticationVerifier, firebaseIdentityRepository, socialIdentityRepository,
				userRepository, accessTokenIssuer, refreshSessionIssuer, enrollmentAttemptService,
				clock, null);
	}

	public FirebaseExchangeService(
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			UserRepository userRepository,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			FirebaseEnrollmentAttemptService enrollmentAttemptService,
			Clock clock,
			UserWithdrawalLifecycleRepository withdrawalLifecycleRepository
	) {
		this.authenticationVerifier = Objects.requireNonNull(
				authenticationVerifier,
				"authenticationVerifier must not be null"
		);
		this.firebaseIdentityRepository = Objects.requireNonNull(
				firebaseIdentityRepository,
				"firebaseIdentityRepository must not be null"
		);
		this.socialIdentityRepository = Objects.requireNonNull(
				socialIdentityRepository,
				"socialIdentityRepository must not be null"
		);
		this.userRepository = Objects.requireNonNull(
				userRepository,
				"userRepository must not be null"
		);
		this.accessTokenIssuer = Objects.requireNonNull(
				accessTokenIssuer,
				"accessTokenIssuer must not be null"
		);
		this.refreshSessionIssuer = Objects.requireNonNull(
				refreshSessionIssuer,
				"refreshSessionIssuer must not be null"
		);
		this.enrollmentAttemptService = Objects.requireNonNull(
				enrollmentAttemptService,
				"enrollmentAttemptService must not be null"
		);
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.withdrawalLifecycleRepository = withdrawalLifecycleRepository;
		this.withdrawalEnrollmentGate = withdrawalLifecycleRepository == null
				? null
				: new WithdrawalEnrollmentGate(userRepository, withdrawalLifecycleRepository);
	}

	@Override
	public FirebaseExchangeResponse exchange(FirebaseExchangeRequest request) {
		FirebaseExchangeRequest requiredRequest = Objects.requireNonNull(
				request,
				"request must not be null"
		);
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.LOGIN_EXCHANGE
		);

		Optional<FirebaseIdentity> firebaseIdentity = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						principal.firebaseProjectId(),
						principal.firebaseUid()
				);
		if (firebaseIdentity.isPresent()) {
			return authenticate(firebaseIdentity.orElseThrow(), principal);
		}

		ensureNoSocialIdentityOwner(principal, null);
		FirebaseEnrollmentAttempt attempt = enrollmentAttemptService.startOrReuse(
				principal.firebaseProjectId(),
				principal.firebaseUid(),
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				principal.signInMethod()
		);
		return new FirebaseEnrollmentRequiredResponse(
				attempt.getEnrollmentId(),
				missingRequirements(principal),
				remainingMillis(attempt)
		);
	}

	private FirebaseAuthenticatedResponse authenticate(
			FirebaseIdentity firebaseIdentity,
			VerifiedFirebasePrincipal principal
	) {
		checkOwner(firebaseIdentity.getUserId());
		User user = userRepository.findById(firebaseIdentity.getUserId())
				.orElseThrow(() -> new AuthException(
						AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT
				));
		if (user.getStatus() != UserStatus.ACTIVE || !user.isMember()) {
			if (user.getStatus() == UserStatus.WITHDRAWN
					&& withdrawalLifecycleRepository != null
					&& withdrawalLifecycleRepository.findByUserId(user.getUserId()).isPresent()) {
				throw new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING);
			}
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}
		ensureNoSocialIdentityOwner(principal, user.getUserId());
		long epoch = refreshSessionIssuer.captureEpoch(firebaseIdentity.getUserId());

		IssuedAccessToken accessToken = accessTokenIssuer.issue(
				user.getUserId(), user.getAccountType(), Set.of()
		);
		IssuedRefreshSession refreshSession = refreshSessionIssuer.isFenceEnabled()
				? refreshSessionIssuer.issueAuthenticated(user.getUserId(),
						refreshSessionIssuer.security().firebase(user.getUserId(), epoch, principal))
				: refreshSessionIssuer.issue(user.getUserId());
		return new FirebaseAuthenticatedResponse(
				accessToken.tokenValue(),
				refreshSession.tokenValue(),
				IssuedAccessToken.BEARER_TOKEN_TYPE,
				Duration.between(accessToken.issuedAt(), accessToken.expiresAt()).toMillis(),
				Duration.between(refreshSession.issuedAt(), refreshSession.expiresAt()).toMillis()
		);
	}

	private void ensureNoSocialIdentityOwner(
			VerifiedFirebasePrincipal principal,
			String expectedUserId
	) {
		for (VerifiedSocialPrincipal socialPrincipal : principal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> socialIdentity = socialIdentityRepository
					.findByProviderAndProviderSubject(
							socialPrincipal.provider(),
							socialPrincipal.providerSubject()
					);
			if (socialIdentity.isPresent()
					&& !Objects.equals(
							expectedUserId,
							socialIdentity.orElseThrow().getUserId()
					)) {
				checkOwner(socialIdentity.orElseThrow().getUserId());
				throw new AuthException(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
		}
	}

	private void checkOwner(String ownerUserId) {
		if (withdrawalEnrollmentGate != null) {
			withdrawalEnrollmentGate.checkExistingOwner(ownerUserId);
		}
	}

	private Set<FirebaseEnrollmentRequirement> missingRequirements(
			VerifiedFirebasePrincipal principal
	) {
		EnumSet<FirebaseEnrollmentRequirement> requirements = EnumSet.noneOf(
				FirebaseEnrollmentRequirement.class
		);
		if (principal.linkedMethods().contains(FirebaseAuthenticationMethod.PASSWORD)
				&& !principal.emailVerified()) {
			requirements.add(FirebaseEnrollmentRequirement.EMAIL_VERIFICATION);
		}
		if (!principal.linkedMethods().contains(FirebaseAuthenticationMethod.PHONE)
				|| !principal.phoneVerified()) {
			requirements.add(FirebaseEnrollmentRequirement.PHONE_VERIFICATION);
		}
		requirements.add(FirebaseEnrollmentRequirement.PROFILE);
		requirements.add(FirebaseEnrollmentRequirement.CONSENTS);
		return Set.copyOf(requirements);
	}

	private long remainingMillis(FirebaseEnrollmentAttempt attempt) {
		long remaining = Duration.between(clock.instant(), attempt.getExpiresAt()).toMillis();
		return Math.max(0, remaining);
	}
}
