package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestUpgradeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKey;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.auth.session.application.TokenReissueService;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

class FirebaseGuestUpgradeServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-18T04:00:00Z");
	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "firebase-uid-sensitive";

	private User guest;
	private FirebaseEnrollmentAttempt attempt;
	private VerifiedFirebasePrincipal principal;
	private FirebaseAuthenticationVerifier verifier;
	private FirebaseIdentityOwnershipService ownershipService;
	private FirebaseGuestUpgradeTransactionService transactionService;
	private RefreshSessionIssuer refreshSessionIssuer;
	private AccessTokenIssuer accessTokenIssuer;
	private FirebaseGuestUpgradeService service;

	@BeforeEach
	void setUp() {
		CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
		UserRepository userRepository = mock(UserRepository.class);
		verifier = mock(FirebaseAuthenticationVerifier.class);
		FirebaseEnrollmentAttemptRepository enrollmentRepository = mock(
				FirebaseEnrollmentAttemptRepository.class
		);
		FirebaseIdentityRepository firebaseIdentityRepository = mock(
				FirebaseIdentityRepository.class
		);
		SocialIdentityRepository socialIdentityRepository = mock(SocialIdentityRepository.class);
		PhoneFingerprintAliasRepository aliasRepository = mock(
				PhoneFingerprintAliasRepository.class
		);
		ownershipService = mock(FirebaseIdentityOwnershipService.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		transactionService = mock(FirebaseGuestUpgradeTransactionService.class);
		accessTokenIssuer = mock(AccessTokenIssuer.class);
		guest = User.createGuest(
				"A".repeat(43),
				"게스트",
				UserConsents.consented(
						"privacy-v1",
						"term-v1",
						true,
						"quality-review-v1",
						NOW.minusSeconds(120)
				),
				NOW.minusSeconds(120)
		);
		attempt = FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.GUEST_USER,
				guest.getUserId(),
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(60),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
		principal = principal();
		when(currentUserProvider.getCurrentUserId()).thenReturn(guest.getUserId());
		when(userRepository.findById(guest.getUserId())).thenReturn(Optional.of(guest));
		when(enrollmentRepository.findById(attempt.getEnrollmentId()))
				.thenReturn(Optional.of(attempt));
		when(verifier.verify(
				"upgrade-credential",
				FirebaseVerificationPurpose.GUEST_ENROLLMENT
		)).thenReturn(principal);
		when(ownershipService.resolve(principal, guest.getUserId()))
				.thenReturn(FirebaseOwnershipOutcome.UNOWNED);
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				PROJECT_ID,
				FIREBASE_UID
		)).thenReturn(Optional.empty());
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.empty());
		when(aliasRepository.findAllActiveByFingerprints(any())).thenReturn(List.of());
		PreparedRefreshSession prepared = new PreparedRefreshSession(
				"refresh-secret",
				RefreshSession.create(
						guest.getUserId(),
						"refresh-hash",
						NOW,
						NOW.plus(Duration.ofDays(14))
				)
		);
		when(refreshSessionIssuer.prepare(guest.getUserId())).thenReturn(prepared);
		when(transactionService.upgrade(
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
		)).thenReturn(new IssuedRefreshSession(
				"refresh-secret",
				NOW,
				NOW.plus(Duration.ofDays(14))
		));
		when(accessTokenIssuer.issue(guest.getUserId(), UserAccountType.MEMBER, Set.of())).thenReturn(
				new IssuedAccessToken(
						"access-secret",
						"Bearer",
						NOW,
						NOW.plus(Duration.ofMinutes(30)),
						1800
				)
		);

		service = new FirebaseGuestUpgradeService(
				currentUserProvider,
				userRepository,
				verifier,
				enrollmentRepository,
				firebaseIdentityRepository,
				socialIdentityRepository,
				aliasRepository,
				ownershipService,
				new PhoneNumberNormalizer(),
				new PhoneFingerprintHasher(registry((byte) 1)),
				new PhoneEligibilityFingerprintHasher("scope-v1", registry((byte) 2)),
				new ConsentPolicy("privacy-v1", "term-v1", "quality-review-v1"),
				refreshSessionIssuer,
				transactionService,
				accessTokenIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);
	}

	@Test
	void promotesSameCanonicalGuestAndIssuesTokensOnlyAfterTransaction() {
		String originalUserId = guest.getUserId();
		Instant expectedUpdatedAt = guest.getUpdatedAt();

		FirebaseSignupResponse response = service.upgrade(request());

		assertThat(response.accessToken()).isEqualTo("access-secret");
		assertThat(response.refreshToken()).isEqualTo("refresh-secret");
		verify(accessTokenIssuer).issue(originalUserId, UserAccountType.MEMBER, Set.of());
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<Instant> expectedUpdatedAtCaptor = ArgumentCaptor.forClass(Instant.class);
		ArgumentCaptor<FirebaseIdentity> firebaseCaptor = ArgumentCaptor.forClass(
				FirebaseIdentity.class
		);
		verify(transactionService).upgrade(
				userCaptor.capture(),
				expectedUpdatedAtCaptor.capture(),
				firebaseCaptor.capture(),
				any(), any(), any(), any(), any(), any(), any()
		);
		User promoted = userCaptor.getValue();
		assertThat(promoted.getUserId()).isEqualTo(originalUserId);
		assertThat(promoted.isMember()).isTrue();
		assertThat(promoted.getGuestInstallationIdHash()).isNull();
		assertThat(promoted.getConsents().isQualityReviewConsented()).isTrue();
		assertThat(expectedUpdatedAtCaptor.getValue()).isEqualTo(expectedUpdatedAt);
		assertThat(firebaseCaptor.getValue().getUserId()).isEqualTo(originalUserId);
	}

	@Test
	void upgradeThenRefreshKeepsSameUserIdAndMemberType() {
		String originalUserId = guest.getUserId();
		FirebaseSignupResponse upgraded = service.upgrade(request());
		RefreshTokenHasher hasher = new RefreshTokenHasher();
		RefreshSession session = RefreshSession.create(originalUserId,
				hasher.hash(upgraded.refreshToken()), NOW, NOW.plus(Duration.ofDays(14)));
		RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
		UserRepository users = mock(UserRepository.class);
		when(sessions.findByTokenHash(hasher.hash(upgraded.refreshToken())))
				.thenReturn(Optional.of(session));
		when(users.findById(originalUserId)).thenReturn(Optional.of(guest));
		when(refreshSessionIssuer.issueRotated(any(), any(), any(), any(), any()))
				.thenReturn(new IssuedRefreshSession("rotated-refresh", NOW, NOW.plus(Duration.ofDays(14))));
		TokenReissueService reissue = new TokenReissueService(hasher, sessions, users,
				accessTokenIssuer, refreshSessionIssuer, new AuthResponseConverter(),
				Clock.fixed(NOW, ZoneOffset.UTC));

		var refreshed = reissue.reissue(new ReissueRequest(upgraded.refreshToken()));

		assertThat(refreshed.accessToken()).isEqualTo("access-secret");
		verify(users).findById(originalUserId);
		verify(accessTokenIssuer, times(2)).issue(originalUserId, UserAccountType.MEMBER, Set.of());
		assertThat(guest.getUserId()).isEqualTo(originalUserId);
	}

	@Test
	void existingMemberOwnerReturnsMergeRequiredWithoutPreparingSessionOrMutation() {
		when(ownershipService.resolve(principal, guest.getUserId()))
				.thenReturn(FirebaseOwnershipOutcome.OWNED_BY_OTHER_ACTIVE_MEMBER);

		assertAuthError(AuthErrorStatus.MERGE_REQUIRED);

		verify(refreshSessionIssuer, never()).prepare(any());
		verify(transactionService, never()).upgrade(
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
		);
		verify(accessTokenIssuer, never()).issue(any(), any(), any());
	}

	@Test
	void mismatchedFreshFirebaseUidRejectsEnrollmentBeforeMutation() {
		when(verifier.verify(any(), any())).thenReturn(new VerifiedFirebasePrincipal(
				PROJECT_ID,
				"other-firebase-uid",
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				true,
				"+821012345678",
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE),
				List.of()
		));

		assertAuthError(AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);

		verify(refreshSessionIssuer, never()).prepare(any());
		verify(transactionService, never()).upgrade(
				any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
		);
	}

	private FirebaseGuestUpgradeRequest request() {
		return new FirebaseGuestUpgradeRequest(
				attempt.getEnrollmentId(),
				"upgrade-credential",
				"승격회원",
				true,
				"privacy-v1",
				true,
				"term-v1"
		);
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
				true,
				"+821012345678",
				Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE),
				List.of(new VerifiedSocialPrincipal(
						SocialProvider.GOOGLE,
						"google-subject-sensitive"
				))
		);
	}

	private PhoneFingerprintKeyRegistry registry(byte fill) {
		byte[] key = new byte[32];
		java.util.Arrays.fill(key, fill);
		return new PhoneFingerprintKeyRegistry(List.of(PhoneFingerprintKey.fromBase64(
				"v1",
				PhoneFingerprintKeyStatus.ACTIVE_WRITE,
				Base64.getEncoder().encodeToString(key)
		)));
	}

	private void assertAuthError(AuthErrorStatus expected) {
		assertThatThrownBy(() -> service.upgrade(request()))
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(expected));
	}
}
