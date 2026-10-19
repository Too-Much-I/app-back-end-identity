package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;

class FirebaseIdentityOwnershipServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-18T03:00:00Z");

	@Test
	void mixedCurrentGuestAndOtherMemberOwnershipIsMergeRequired() {
		FirebaseIdentityRepository firebaseRepository = mock(FirebaseIdentityRepository.class);
		SocialIdentityRepository socialRepository = mock(SocialIdentityRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		FirebaseIdentityOwnershipService service = new FirebaseIdentityOwnershipService(
				firebaseRepository,
				socialRepository,
				userRepository
		);
		User guest = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		User member = User.createFederatedMember(
				"회원",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		VerifiedFirebasePrincipal principal = new VerifiedFirebasePrincipal(
				"test-project",
				"firebase-uid-sensitive",
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				false,
				null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of(new VerifiedSocialPrincipal(
						SocialProvider.GOOGLE,
						"google-subject-sensitive"
				))
		);
		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project",
				"firebase-uid-sensitive"
		)).thenReturn(Optional.of(FirebaseIdentity.create(
				"test-project",
				"firebase-uid-sensitive",
				guest.getUserId(),
				NOW
		)));
		when(socialRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.of(SocialIdentity.create(
				member.getUserId(),
				SocialProvider.GOOGLE,
				"google-subject-sensitive",
				NOW
		)));
		when(userRepository.findById(member.getUserId())).thenReturn(Optional.of(member));

		assertThat(service.resolve(principal, guest.getUserId()))
				.isEqualTo(FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER);
	}

	@Test
	void withdrawnOwnerStopsGuestPrepareAndUpgradeOwnershipResolution() {
		FirebaseIdentityRepository firebaseRepository = mock(FirebaseIdentityRepository.class);
		SocialIdentityRepository socialRepository = mock(SocialIdentityRepository.class);
		UserRepository userRepository = mock(UserRepository.class);
		WithdrawalEnrollmentGate gate = mock(WithdrawalEnrollmentGate.class);
		FirebaseIdentityOwnershipService service = new FirebaseIdentityOwnershipService(
				firebaseRepository,
				socialRepository,
				userRepository,
				gate
		);
		User guest = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		User owner = User.createFederatedMember(
				"탈퇴처리중",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		when(firebaseRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project", "firebase-uid-sensitive"
		)).thenReturn(Optional.of(FirebaseIdentity.create(
				"test-project", "firebase-uid-sensitive", owner.getUserId(), NOW
		)));
		doThrow(new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING))
				.when(gate).checkExistingOwner(owner.getUserId());
		VerifiedFirebasePrincipal principal = new VerifiedFirebasePrincipal(
				"test-project",
				"firebase-uid-sensitive",
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				false,
				null,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of()
		);

		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> service.resolve(principal, guest.getUserId())
		).isInstanceOfSatisfying(AuthException.class, exception ->
				assertThat(exception.getErrorCode())
						.isEqualTo(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING));
	}
}
