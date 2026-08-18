package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestPrepareRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestPrepareResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestPrepareResultType;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

class FirebaseGuestPrepareServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-18T03:00:00Z");
	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "firebase-uid-sensitive";

	private CurrentUserProvider currentUserProvider;
	private UserRepository userRepository;
	private FirebaseAuthenticationVerifier verifier;
	private FirebaseIdentityOwnershipService ownershipService;
	private FirebaseEnrollmentAttemptService enrollmentAttemptService;
	private FirebaseGuestPrepareService service;
	private User guest;
	private VerifiedFirebasePrincipal principal;

	@BeforeEach
	void setUp() {
		currentUserProvider = mock(CurrentUserProvider.class);
		userRepository = mock(UserRepository.class);
		verifier = mock(FirebaseAuthenticationVerifier.class);
		ownershipService = mock(FirebaseIdentityOwnershipService.class);
		enrollmentAttemptService = mock(FirebaseEnrollmentAttemptService.class);
		service = new FirebaseGuestPrepareService(
				currentUserProvider,
				userRepository,
				verifier,
				ownershipService,
				enrollmentAttemptService,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
		guest = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented("privacy-v1", "term-v1", NOW.minusSeconds(60)),
				NOW.minusSeconds(60)
		);
		principal = principal();
		when(currentUserProvider.getCurrentUserId()).thenReturn(guest.getUserId());
		when(userRepository.findById(guest.getUserId())).thenReturn(Optional.of(guest));
		when(verifier.verify(
				"prepare-credential",
				FirebaseVerificationPurpose.GUEST_ENROLLMENT_PREPARE
		)).thenReturn(principal);
	}

	@Test
	void createsGuestBoundEnrollmentFromJwtSubjectWithoutPhoneProof() {
		FirebaseEnrollmentAttempt attempt = FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				guest.getUserId(),
				FirebaseAuthenticationMethod.GOOGLE,
				NOW,
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
		when(ownershipService.resolve(principal, guest.getUserId()))
				.thenReturn(FirebaseOwnershipOutcome.UNOWNED);
		when(enrollmentAttemptService.startOrReuse(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				guest.getUserId(),
				FirebaseAuthenticationMethod.GOOGLE
		)).thenReturn(attempt);

		FirebaseGuestPrepareResponse response = service.prepare(
				new FirebaseGuestPrepareRequest("prepare-credential")
		);

		assertThat(response.type()).isEqualTo(FirebaseGuestPrepareResultType.ENROLLMENT_REQUIRED);
		assertThat(response.enrollmentId()).isEqualTo(attempt.getEnrollmentId());
		assertThat(response.expiresIn()).isEqualTo(Duration.ofMinutes(10).toMillis());
	}

	@Test
	void returnsMergeRequiredWithoutCreatingEnrollmentForOtherActiveMemberOwner() {
		when(ownershipService.resolve(principal, guest.getUserId()))
				.thenReturn(FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER);

		FirebaseGuestPrepareResponse response = service.prepare(
				new FirebaseGuestPrepareRequest("prepare-credential")
		);

		assertThat(response.type()).isEqualTo(FirebaseGuestPrepareResultType.MERGE_REQUIRED);
		verify(enrollmentAttemptService, never()).startOrReuse(any(), any(), any(), any(), any());
	}

	@Test
	void rejectsMemberBeforeVerifyingFirebaseCredential() {
		User member = User.createFederatedMember(
				"회원",
				UserConsents.consented("privacy-v1", "term-v1", NOW),
				NOW
		);
		when(currentUserProvider.getCurrentUserId()).thenReturn(member.getUserId());
		when(userRepository.findById(member.getUserId())).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> service.prepare(
				new FirebaseGuestPrepareRequest("prepare-credential")
		))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.GUEST_UPGRADE_NOT_ALLOWED));
		verify(verifier, never()).verify(any(), any());
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
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of()
		);
	}
}
