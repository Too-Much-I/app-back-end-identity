package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseVerificationPurpose;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedSocialPrincipal;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.global.exception.BusinessException;

class FirebaseWithdrawalCredentialVerifierTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String OTHER_USER_ID = "f59fd214-4938-46eb-9ea5-bafc29ecbd27";
	private static final Instant NOW = Instant.parse("2026-08-25T01:02:03Z");

	private FirebaseAuthenticationVerifier authenticationVerifier;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private FirebaseWithdrawalCredentialVerifier verifier;
	private FirebaseIdentity identity;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		ObjectProvider<FirebaseAuthenticationVerifier> provider = mock(ObjectProvider.class);
		ObjectProvider<FirebaseIdentityRepository> identityProvider = mock(ObjectProvider.class);
		ObjectProvider<SocialIdentityRepository> socialProvider = mock(ObjectProvider.class);
		authenticationVerifier = mock(FirebaseAuthenticationVerifier.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		when(provider.getIfAvailable()).thenReturn(authenticationVerifier);
		when(identityProvider.getIfAvailable()).thenReturn(firebaseIdentityRepository);
		when(socialProvider.getIfAvailable()).thenReturn(socialIdentityRepository);
		verifier = new FirebaseWithdrawalCredentialVerifier(
				provider, identityProvider, socialProvider
		);
		identity = FirebaseIdentity.create("firebase-project", "firebase-uid", USER_ID, NOW);
	}

	@Test
	void returnsRedactedTargetWhenFirebaseAndSocialOwnersMatch() {
		VerifiedFirebasePrincipal principal = principal();
		when(authenticationVerifier.verify("fresh-proof", FirebaseVerificationPurpose.WITHDRAWAL))
				.thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"firebase-project", "firebase-uid"
		)).thenReturn(Optional.of(identity));
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE, "google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				USER_ID, SocialProvider.GOOGLE, "google-subject", NOW
		)));

		FirebaseWithdrawalTarget target = verifier.verify(USER_ID, identity, "fresh-proof");

		assertThat(target.firebaseProjectId()).isEqualTo("firebase-project");
		assertThat(target.firebaseUid()).isEqualTo("firebase-uid");
	}

	@Test
	void rejectsLinkedSocialIdentityOwnedByAnotherUser() {
		when(authenticationVerifier.verify("fresh-proof", FirebaseVerificationPurpose.WITHDRAWAL))
				.thenReturn(principal());
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"firebase-project", "firebase-uid"
		)).thenReturn(Optional.of(identity));
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE, "google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				OTHER_USER_ID, SocialProvider.GOOGLE, "google-subject", NOW
		)));

		BusinessException exception = catchThrowableOfType(
				BusinessException.class,
				() -> verifier.verify(USER_ID, identity, "fresh-proof")
		);
		assertThat(exception.getErrorCode()).isEqualTo(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}

	private VerifiedFirebasePrincipal principal() {
		return new VerifiedFirebasePrincipal(
				"firebase-project", "firebase-uid", FirebaseAuthenticationMethod.GOOGLE,
				NOW, NOW, NOW.plusSeconds(300), false, false, null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google-subject"))
		);
	}
}
