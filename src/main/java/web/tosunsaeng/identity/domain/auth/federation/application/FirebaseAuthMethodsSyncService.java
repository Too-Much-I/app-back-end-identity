package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;

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
		long expectedEpoch = sessionSecurity == null ? 0 : sessionSecurity.captureEpoch(userId);
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
		Instant now = clock.instant();
		List<SocialIdentity> missing = new ArrayList<>();
		for (VerifiedSocialPrincipal social : principal.linkedSocialPrincipals()) {
			Optional<SocialIdentity> existing = socialIdentityRepository
					.findByProviderAndProviderSubject(
						social.provider(),
						social.providerSubject()
					);
			if (existing.isEmpty()) {
				missing.add(SocialIdentity.create(
						userId,
						social.provider(),
						social.providerSubject(),
						now
				));
			} else if (!userId.equals(existing.orElseThrow().getUserId())) {
				checkOwner(existing.orElseThrow().getUserId());
				throw new AuthException(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT);
			}
		}
		try {
			if (sessionSecurity == null) transactionService.saveMissing(missing);
			else sessionSecurity.transactionKeepingUniqueConflicts(() -> {
				var proof = sessionSecurity.firebase(userId, expectedEpoch, principal);
				sessionSecurity.checkFirebaseAuthentication(userId, proof);
				transactionService.saveMissing(missing);
				return null;
			});
		} catch (DuplicateKeyException exception) {
			ensureAllPrincipalSocialIdentitiesOwnedBy(principal, userId);
		}
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
