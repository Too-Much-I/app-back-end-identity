package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;


import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseAuthMethodsSyncRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthMethodsSyncResponse;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

public final class FirebaseAuthMethodsSyncService implements FirebaseAuthMethodsSyncUseCase {
	private web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeService providerChanges;
	@Autowired(required = false)
	public void setProviderChanges(web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeService service) { providerChanges = service; }
	private web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard providerChangeGuard;
	@Autowired(required = false)
	public void setProviderChangeGuard(web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeGuard guard) { providerChangeGuard = guard; }
	private SessionSecurityService sessionSecurity;
	@Autowired(required = false)
	public void setSessionSecurity(SessionSecurityService security) { sessionSecurity = security; }

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final SocialIdentityRepository socialIdentityRepository;
	private final FirebaseAuthenticationVerifier authenticationVerifier;
	private final FirebaseAuthMethodsSyncTransactionService transactionService;
	private final Clock clock;
	private final WithdrawalEnrollmentGate withdrawalEnrollmentGate;

	public FirebaseAuthMethodsSyncService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseAuthMethodsSyncTransactionService transactionService,
			Clock clock
	) {
		this(currentUserProvider, userRepository, firebaseIdentityRepository,
				socialIdentityRepository, authenticationVerifier, transactionService,
				clock, null);
	}

	public FirebaseAuthMethodsSyncService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository,
			SocialIdentityRepository socialIdentityRepository,
			FirebaseAuthenticationVerifier authenticationVerifier,
			FirebaseAuthMethodsSyncTransactionService transactionService,
			Clock clock,
			WithdrawalEnrollmentGate withdrawalEnrollmentGate
	) {
		this.currentUserProvider = Objects.requireNonNull(currentUserProvider);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
		this.socialIdentityRepository = Objects.requireNonNull(socialIdentityRepository);
		this.authenticationVerifier = Objects.requireNonNull(authenticationVerifier);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.clock = Objects.requireNonNull(clock);
		this.withdrawalEnrollmentGate = withdrawalEnrollmentGate;
	}

	@Override
	public FirebaseAuthMethodsSyncResponse sync(FirebaseAuthMethodsSyncRequest request) {
		FirebaseAuthMethodsSyncRequest requiredRequest = Objects.requireNonNull(request);
		String userId = currentUserProvider.getCurrentUserId();
		if (providerChanges == null && providerChangeGuard != null && providerChangeGuard.hasState(userId)) {
			throw new AuthException(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		}
		long expectedEpoch = sessionSecurity == null ? 0 : sessionSecurity.captureEpoch(userId);
		long expectedChangeRevision = providerChanges == null ? 0 : providerChanges.captureRevision(userId);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
		if (user.getStatus() != UserStatus.ACTIVE || !user.isMember()) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}
		FirebaseIdentity firebaseIdentity = firebaseIdentityRepository.findByUserId(userId)
				.orElseThrow(() -> new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		VerifiedFirebasePrincipal principal = authenticationVerifier.verify(
				requiredRequest.firebaseIdToken(),
				FirebaseVerificationPurpose.AUTH_METHOD_SYNC
		);
		if (!firebaseIdentity.getFirebaseProjectId().equals(principal.firebaseProjectId())
				|| !firebaseIdentity.getFirebaseUid().equals(principal.firebaseUid())) {
			throw new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
		}
		// Retained endpoint is validation-only. Both first links and relinks use common link/complete.
		if (requiredRequest.linkAttemptId() != null) throw new AuthException(AuthErrorStatus.PROVIDER_RELINK_REQUIRED);
		ensureAllPrincipalSocialIdentitiesOwnedBy(principal, userId);
		if (sessionSecurity != null) sessionSecurity.transactionKeepingUniqueConflicts(() -> {
			if (providerChanges != null) providerChanges.synchronize(userId, expectedEpoch, expectedChangeRevision, principal, null, () -> {});
			else {
				if (providerChangeGuard != null && providerChangeGuard.hasState(userId)) throw new AuthException(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
				sessionSecurity.checkFirebaseAuthentication(userId, sessionSecurity.firebase(userId, expectedEpoch, principal));
			}
			return null;
		});

		return new FirebaseAuthMethodsSyncResponse(linkedProviders(userId));
	}

	private void ensureAllPrincipalSocialIdentitiesOwnedBy(
			VerifiedFirebasePrincipal principal,
			String userId
	) {
		for (VerifiedSocialPrincipal social : principal.linkedSocialPrincipals()) {
			SocialIdentity existing = socialIdentityRepository
					.findByProviderAndProviderSubject(
							social.provider(),
							social.providerSubject()
					)
					.orElseThrow(() -> new AuthException(
							AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT
					));
			if (!userId.equals(existing.getUserId())) {
				checkOwner(existing.getUserId());
				throw new AuthException(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
		}
	}

	private Set<SocialProvider> linkedProviders(String userId) {
		EnumSet<SocialProvider> providers = EnumSet.noneOf(SocialProvider.class);
		for (SocialIdentity identity : socialIdentityRepository.findAllByUserId(userId)) {
			providers.add(identity.getProvider());
		}
		return Set.copyOf(providers);
	}

	private void checkOwner(String ownerUserId) {
		if (withdrawalEnrollmentGate != null) {
			withdrawalEnrollmentGate.checkExistingOwner(ownerUserId);
		}
	}
}
