package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthenticatedResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequiredResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseEnrollmentRequirement;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

class FirebaseExchangeServiceTests {

	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "opaque-firebase-uid";
	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String OTHER_USER_ID = "45c05c3f-ae7f-4ca7-af88-3ab8aa8f428e";
	private static final Instant NOW = Instant.parse("2026-08-14T03:00:00Z");
	private static final Duration ENROLLMENT_TTL = Duration.ofMinutes(10);

	private FirebaseAuthenticationVerifier authenticationVerifier;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private UserRepository userRepository;
	private AccessTokenIssuer accessTokenIssuer;
	private RefreshSessionIssuer refreshSessionIssuer;
	private FirebaseEnrollmentAttemptService enrollmentAttemptService;
	private FirebaseExchangeService service;

	@BeforeEach
	void setUp() {
		authenticationVerifier = mock(FirebaseAuthenticationVerifier.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		userRepository = mock(UserRepository.class);
		accessTokenIssuer = mock(AccessTokenIssuer.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		enrollmentAttemptService = mock(FirebaseEnrollmentAttemptService.class);
		service = new FirebaseExchangeService(
				authenticationVerifier,
				firebaseIdentityRepository,
				socialIdentityRepository,
				userRepository,
				accessTokenIssuer,
				refreshSessionIssuer,
				enrollmentAttemptService,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void existingFirebaseIdentityIssuesIdentityAccessAndRefreshTokensForActiveMember() {
		VerifiedFirebasePrincipal principal = principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				true,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE),
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google-subject"))
		);
		FirebaseIdentity identity = FirebaseIdentity.create(
				PROJECT_ID,
				FIREBASE_UID,
				USER_ID,
				NOW.minusSeconds(60)
		);
		User user = member(UserStatus.ACTIVE);
		when(authenticationVerifier.verify("firebase-test-credential", FirebaseVerificationPurpose.LOGIN_EXCHANGE))
				.thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.of(identity));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				USER_ID,
				SocialProvider.GOOGLE,
				"google-subject",
				NOW.minusSeconds(60)
		)));
		when(accessTokenIssuer.issue(USER_ID, Set.of())).thenReturn(new IssuedAccessToken(
				"identity-access-token",
				"Bearer",
				NOW,
				NOW.plus(Duration.ofMinutes(30)),
				1800
		));
		when(refreshSessionIssuer.issue(USER_ID)).thenReturn(new IssuedRefreshSession(
				"identity-refresh-token",
				NOW,
				NOW.plus(Duration.ofDays(14))
		));

		FirebaseAuthenticatedResponse response = (FirebaseAuthenticatedResponse) service.exchange(
				new FirebaseExchangeRequest("firebase-test-credential")
		);

		assertThat(response.type().name()).isEqualTo("AUTHENTICATED");
		assertThat(response.accessToken()).isEqualTo("identity-access-token");
		assertThat(response.refreshToken()).isEqualTo("identity-refresh-token");
		assertThat(response.accessTokenExpiresIn()).isEqualTo(1_800_000);
		assertThat(response.refreshTokenExpiresIn()).isEqualTo(1_209_600_000);
		verify(accessTokenIssuer).issue(USER_ID, Set.of());
		verify(refreshSessionIssuer).issue(USER_ID);
		verifyNoInteractions(enrollmentAttemptService);
	}

	@Test
	void missingFirebaseIdentityReturnsEnrollmentWithoutCreatingUserOrSession() {
		VerifiedFirebasePrincipal principal = principal(
				FirebaseAuthenticationMethod.PASSWORD,
				false,
				false,
				Set.of(FirebaseAuthenticationMethod.PASSWORD),
				List.of()
		);
		FirebaseEnrollmentAttempt attempt = directAttempt(NOW);
		when(authenticationVerifier.verify("firebase-test-credential", FirebaseVerificationPurpose.LOGIN_EXCHANGE))
				.thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.empty());
		when(enrollmentAttemptService.startOrReuse(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.PASSWORD
		)).thenReturn(attempt);

		FirebaseEnrollmentRequiredResponse response = (FirebaseEnrollmentRequiredResponse) service
				.exchange(new FirebaseExchangeRequest("firebase-test-credential"));

		assertThat(response.type().name()).isEqualTo("ENROLLMENT_REQUIRED");
		assertThat(response.enrollmentId()).isEqualTo(attempt.getEnrollmentId());
		assertThat(response.expiresIn()).isEqualTo(600_000);
		assertThat(response.missingRequirements()).containsExactlyInAnyOrder(
				FirebaseEnrollmentRequirement.EMAIL_VERIFICATION,
				FirebaseEnrollmentRequirement.PHONE_VERIFICATION,
				FirebaseEnrollmentRequirement.PROFILE,
				FirebaseEnrollmentRequirement.CONSENTS
		);
		verify(userRepository, never()).save(any(User.class));
		verifyNoInteractions(accessTokenIssuer, refreshSessionIssuer);
	}

	@Test
	void verifiedPhoneOmitsPhoneRequirementButProfileAndConsentsRemainRequired() {
		VerifiedFirebasePrincipal principal = principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				true,
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE),
				List.of()
		);
		when(authenticationVerifier.verify(any(), any())).thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.empty());
		when(enrollmentAttemptService.startOrReuse(any(), any(), any(), any(), any()))
				.thenReturn(directAttempt(NOW));

		FirebaseEnrollmentRequiredResponse response = (FirebaseEnrollmentRequiredResponse) service
				.exchange(new FirebaseExchangeRequest("firebase-test-credential"));

		assertThat(response.missingRequirements()).containsExactlyInAnyOrder(
				FirebaseEnrollmentRequirement.PROFILE,
				FirebaseEnrollmentRequirement.CONSENTS
		);
	}

	@Test
	void existingSocialOwnerWithoutFirebaseIdentityFailsClosedWithoutEnrollment() {
		VerifiedFirebasePrincipal principal = principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				false,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google-subject"))
		);
		when(authenticationVerifier.verify(any(), any())).thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.empty());
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				OTHER_USER_ID,
				SocialProvider.GOOGLE,
				"google-subject",
				NOW
		)));

		assertThatThrownBy(() -> service.exchange(
				new FirebaseExchangeRequest("firebase-test-credential")
		))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT));
		verifyNoInteractions(enrollmentAttemptService, accessTokenIssuer, refreshSessionIssuer);
	}

	@Test
	void socialIdentityOwnedByAnotherUserBlocksExistingFirebaseOwnerLogin() {
		VerifiedFirebasePrincipal principal = principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				false,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of(new VerifiedSocialPrincipal(SocialProvider.GOOGLE, "google-subject"))
		);
		User activeMember = member(UserStatus.ACTIVE);
		when(authenticationVerifier.verify(any(), any())).thenReturn(principal);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.of(FirebaseIdentity.create(PROJECT_ID, FIREBASE_UID, USER_ID, NOW)));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeMember));
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject"
		)).thenReturn(Optional.of(SocialIdentity.create(
				OTHER_USER_ID,
				SocialProvider.GOOGLE,
				"google-subject",
				NOW
		)));

		assertThatThrownBy(() -> service.exchange(new FirebaseExchangeRequest("credential")))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.SOCIAL_IDENTITY_CONFLICT));
		verifyNoInteractions(accessTokenIssuer, refreshSessionIssuer, enrollmentAttemptService);
	}

	@Test
	void inconsistentFirebaseIdentityWithoutCanonicalUserFailsClosed() {
		when(authenticationVerifier.verify(any(), any())).thenReturn(principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				false,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of()
		));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.of(FirebaseIdentity.create(PROJECT_ID, FIREBASE_UID, USER_ID, NOW)));
		when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.exchange(
				new FirebaseExchangeRequest("firebase-test-credential")
		))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
		verifyNoInteractions(accessTokenIssuer, refreshSessionIssuer, enrollmentAttemptService);
	}

	@Test
	void inactiveMemberAndGuestCannotReceiveTokens() {
		FirebaseIdentity identity = FirebaseIdentity.create(PROJECT_ID, FIREBASE_UID, USER_ID, NOW);
		when(authenticationVerifier.verify(any(), any())).thenReturn(principal(
				FirebaseAuthenticationMethod.GOOGLE,
				true,
				false,
				Set.of(FirebaseAuthenticationMethod.GOOGLE),
				List.of()
		));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(PROJECT_ID, FIREBASE_UID))
				.thenReturn(Optional.of(identity));
		User suspendedMember = member(UserStatus.SUSPENDED);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(suspendedMember));

		assertThatThrownBy(() -> service.exchange(new FirebaseExchangeRequest("credential")))
				.isInstanceOf(UserException.class);

		User withdrawnMember = member(UserStatus.WITHDRAWN);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(withdrawnMember));

		assertThatThrownBy(() -> service.exchange(new FirebaseExchangeRequest("credential")))
				.isInstanceOf(UserException.class);

		User guest = mock(User.class);
		when(guest.getStatus()).thenReturn(UserStatus.ACTIVE);
		when(guest.isMember()).thenReturn(false);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(guest));

		assertThatThrownBy(() -> service.exchange(new FirebaseExchangeRequest("credential")))
				.isInstanceOf(UserException.class);
		verifyNoInteractions(accessTokenIssuer, refreshSessionIssuer, enrollmentAttemptService);
	}

	@Test
	void verifierFailureStopsBeforeAnyRepositoryOrIssuerCall() {
		when(authenticationVerifier.verify(any(), any()))
				.thenThrow(new AuthException(AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED));

		assertThatThrownBy(() -> service.exchange(new FirebaseExchangeRequest("credential")))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED));
		verifyNoInteractions(
				firebaseIdentityRepository,
				socialIdentityRepository,
				userRepository,
				accessTokenIssuer,
				refreshSessionIssuer,
				enrollmentAttemptService
		);
	}

	@Test
	void disabledUseCaseAlwaysReturnsStableUnavailableErrorWithoutInspectingRequest() {
		DisabledFirebaseExchangeUseCase disabled = new DisabledFirebaseExchangeUseCase();

		assertThatThrownBy(() -> disabled.exchange(null))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(AuthErrorStatus.FIREBASE_UNAVAILABLE));
	}

	private VerifiedFirebasePrincipal principal(
			FirebaseAuthenticationMethod signInMethod,
			boolean emailVerified,
			boolean phoneVerified,
			Set<FirebaseAuthenticationMethod> linkedMethods,
			List<VerifiedSocialPrincipal> socialPrincipals
	) {
		return new VerifiedFirebasePrincipal(
				PROJECT_ID,
				FIREBASE_UID,
				signInMethod,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				emailVerified,
				phoneVerified,
				phoneVerified ? "+820000000000" : null,
				linkedMethods,
				socialPrincipals
		);
	}

	private FirebaseEnrollmentAttempt directAttempt(Instant createdAt) {
		return FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE,
				createdAt,
				ENROLLMENT_TTL,
				Duration.ofHours(24)
		);
	}

	private User member(UserStatus status) {
		User user = mock(User.class);
		when(user.getUserId()).thenReturn(USER_ID);
		when(user.getStatus()).thenReturn(status);
		when(user.isMember()).thenReturn(true);
		return user;
	}
}
