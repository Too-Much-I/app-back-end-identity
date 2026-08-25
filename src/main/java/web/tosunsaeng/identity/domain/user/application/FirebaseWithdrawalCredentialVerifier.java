package web.tosunsaeng.identity.domain.user.application;

import java.util.Objects;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseVerificationPurpose;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedSocialPrincipal;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;

@Component
public class FirebaseWithdrawalCredentialVerifier {

	private final ObjectProvider<FirebaseAuthenticationVerifier> verifierProvider;
	private final ObjectProvider<FirebaseIdentityRepository> firebaseIdentityRepositoryProvider;
	private final ObjectProvider<SocialIdentityRepository> socialIdentityRepositoryProvider;
	private final boolean enabled;

	@Autowired
	public FirebaseWithdrawalCredentialVerifier(
			ObjectProvider<FirebaseAuthenticationVerifier> verifierProvider,
			ObjectProvider<FirebaseIdentityRepository> firebaseIdentityRepositoryProvider,
			ObjectProvider<SocialIdentityRepository> socialIdentityRepositoryProvider,
			@Value("${app.firebase-withdrawal.enabled:false}") boolean enabled
	) {
		this.verifierProvider = Objects.requireNonNull(verifierProvider);
		this.firebaseIdentityRepositoryProvider = Objects.requireNonNull(
				firebaseIdentityRepositoryProvider
		);
		this.socialIdentityRepositoryProvider = Objects.requireNonNull(
				socialIdentityRepositoryProvider
		);
		this.enabled = enabled;
	}

	public FirebaseWithdrawalCredentialVerifier(
			ObjectProvider<FirebaseAuthenticationVerifier> verifierProvider,
			ObjectProvider<FirebaseIdentityRepository> firebaseIdentityRepositoryProvider,
			ObjectProvider<SocialIdentityRepository> socialIdentityRepositoryProvider
	) {
		this(verifierProvider, firebaseIdentityRepositoryProvider,
				socialIdentityRepositoryProvider, true);
	}

	public FirebaseWithdrawalTarget verify(
			String currentUserId,
			FirebaseIdentity expectedIdentity,
			String firebaseIdToken
	) {
		if (!enabled) {
			throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
		}
		FirebaseAuthenticationVerifier verifier = verifierProvider.getIfAvailable();
		if (verifier == null) {
			throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
		}
		FirebaseIdentityRepository firebaseIdentityRepository =
				firebaseIdentityRepositoryProvider.getIfAvailable();
		SocialIdentityRepository socialIdentityRepository =
				socialIdentityRepositoryProvider.getIfAvailable();
		if (firebaseIdentityRepository == null || socialIdentityRepository == null) {
			throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
		}
		VerifiedFirebasePrincipal principal = verifier.verify(
				firebaseIdToken,
				FirebaseVerificationPurpose.WITHDRAWAL
		);
		if (!expectedIdentity.getUserId().equals(currentUserId)
				|| !expectedIdentity.getFirebaseProjectId().equals(principal.firebaseProjectId())
				|| !expectedIdentity.getFirebaseUid().equals(principal.firebaseUid())) {
			throw conflict();
		}
		FirebaseIdentity persistedIdentity = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						principal.firebaseProjectId(), principal.firebaseUid()
				)
				.orElseThrow(this::conflict);
		if (!persistedIdentity.getUserId().equals(currentUserId)) {
			throw conflict();
		}
		for (VerifiedSocialPrincipal socialPrincipal : principal.linkedSocialPrincipals()) {
			SocialIdentity socialIdentity = socialIdentityRepository
					.findByProviderAndProviderSubject(
							socialPrincipal.provider(), socialPrincipal.providerSubject()
					)
					.orElse(null);
			if (socialIdentity != null && !socialIdentity.getUserId().equals(currentUserId)) {
				throw conflict();
			}
		}
		return new FirebaseWithdrawalTarget(principal.firebaseProjectId(), principal.firebaseUid());
	}

	private AuthException conflict() {
		return new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}
}
