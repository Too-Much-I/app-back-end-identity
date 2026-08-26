package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

class FirebaseAuthMethodsSyncServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-18T05:00:00Z");
	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "firebase-uid-sensitive";

	private SocialIdentityRepository socialRepository;
	private FirebaseAuthenticationVerifier verifier;
	private FirebaseAuthMethodsSyncTransactionService transactionService;
	private FirebaseAuthMethodsSyncService service;
	private User member;
	private VerifiedFirebasePrincipal principal;
	private WithdrawalEnrollmentGate withdrawalEnrollmentGate;

	@BeforeEach
	void setUp() {
		CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
		UserRepository userRepository = mock(UserRepository.class);
		FirebaseIdentityRepository firebaseRepository = mock(FirebaseIdentityRepository.class);
		socialRepository = mock(SocialIdentityRepository.class);
		verifier = mock(FirebaseAuthenticationVerifier.class);
		transactionService = mock(FirebaseAuthMethodsSyncTransactionService.class);
		withdrawalEnrollmentGate = mock(WithdrawalEnrollmentGate.class);
		member = User.createFederatedMember(
				"회원",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		principal = principal();
		when(currentUserProvider.getCurrentUserId()).thenReturn(member.getUserId());
		when(userRepository.findById(member.getUserId())).thenReturn(Optional.of(member));
		when(firebaseRepository.findByUserId(member.getUserId())).thenReturn(Optional.of(
				FirebaseIdentity.create(PROJECT_ID, FIREBASE_UID, member.getUserId(), NOW)
		));
		when(verifier.verify(
				"sync-credential",
				FirebaseVerificationPurpose.AUTH_METHOD_SYNC
		)).thenReturn(principal);
		service = new FirebaseAuthMethodsSyncService(
				currentUserProvider,
				userRepository,
				firebaseRepository,
				socialRepository,
				verifier,
				transactionService,
				Clock.fixed(NOW, ZoneOffset.UTC),
				withdrawalEnrollmentGate
		);
	}

	@Test
	@SuppressWarnings("unchecked")
	void addsOnlyMissingSocialIdentityToCurrentCanonicalMember() {
		SocialIdentity google = SocialIdentity.create(
				member.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		);
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.of(google));
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.APPLE,
				"apple-subject-sensitive"
		)).thenReturn(Optional.empty());
		when(socialRepository.findAllByUserId(member.getUserId())).thenReturn(List.of(
				google,
				SocialIdentity.create(
						member.getUserId(),
						SocialProvider.APPLE,
						"apple-subject-sensitive",
						NOW
				)
		));

		FirebaseAuthMethodsSyncResponse response = service.sync(
				new FirebaseAuthMethodsSyncRequest("sync-credential")
		);

		ArgumentCaptor<List<SocialIdentity>> captor = ArgumentCaptor.forClass(List.class);
		verify(transactionService).saveMissing(captor.capture());
		assertThat(captor.getValue())
				.singleElement()
				.satisfies(identity -> {
					assertThat(identity.getUserId()).isEqualTo(member.getUserId());
					assertThat(identity.getProvider()).isEqualTo(SocialProvider.APPLE);
				});
		assertThat(response.linkedProviders())
				.containsExactlyInAnyOrder(SocialProvider.GOOGLE, SocialProvider.APPLE);
	}

	@Test
	void repeatedSyncIsIdempotentAndCreatesNoDuplicateIdentity() {
		when(socialRepository.findByProviderAndProviderSubject(any(), any()))
				.thenAnswer(invocation -> Optional.of(SocialIdentity.create(
						member.getUserId(),
						invocation.getArgument(0),
						invocation.getArgument(1),
						NOW
				)));
		when(socialRepository.findAllByUserId(member.getUserId())).thenReturn(List.of(
				SocialIdentity.create(
						member.getUserId(),
						SocialProvider.GOOGLE,
						"google-subject-sensitive",
						NOW
				),
				SocialIdentity.create(
						member.getUserId(),
						SocialProvider.APPLE,
						"apple-subject-sensitive",
						NOW
				)
		));

		FirebaseAuthMethodsSyncResponse response = service.sync(
				new FirebaseAuthMethodsSyncRequest("sync-credential")
		);

		verify(transactionService).saveMissing(List.of());
		assertThat(response.linkedProviders()).hasSize(2);
	}

	@Test
	void otherUserSocialOwnerRejectsWithoutMutation() {
		User other = User.createFederatedMember(
				"다른회원",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.of(SocialIdentity.create(
				other.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		)));

		assertThatThrownBy(() -> service.sync(
				new FirebaseAuthMethodsSyncRequest("sync-credential")
		))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT));
		verify(transactionService, never()).saveMissing(any());
	}

	@Test
	void withdrawnSocialOwnerUsesCleanupPendingInsteadOfGenericConflict() {
		User other = User.createFederatedMember(
				"탈퇴처리중",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.of(SocialIdentity.create(
				other.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		)));
		doThrow(new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING))
				.when(withdrawalEnrollmentGate).checkExistingOwner(other.getUserId());

		assertThatThrownBy(() -> service.sync(
				new FirebaseAuthMethodsSyncRequest("sync-credential")
		))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						assertThat(exception.getErrorCode())
								.isEqualTo(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING));
		verify(transactionService, never()).saveMissing(any());
	}

	private VerifiedFirebasePrincipal principal() {
		return new VerifiedFirebasePrincipal(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				false,
				null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.APPLE),
				List.of(
						new VerifiedSocialPrincipal(
								SocialProvider.GOOGLE,
								"google-subject-sensitive"
						),
						new VerifiedSocialPrincipal(
								SocialProvider.APPLE,
								"apple-subject-sensitive"
						)
				)
		);
	}
}
