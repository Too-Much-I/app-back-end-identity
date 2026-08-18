package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.mockito.InOrder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.SocialIdentityRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKey;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintSet;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneNumberNormalizer;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.PreparedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.UserFactory;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

class FirebaseSignupServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-14T05:00:00Z");
	private static final String PROJECT_ID = "test-project";
	private static final String FIREBASE_UID = "firebase-uid-sensitive";
	private static final String PHONE = "+821012345678";

	private FirebaseAuthenticationVerifier verifier;
	private FirebaseEnrollmentAttemptRepository enrollmentRepository;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private SocialIdentityRepository socialIdentityRepository;
	private PhoneFingerprintAliasRepository aliasRepository;
	private RefreshSessionIssuer refreshSessionIssuer;
	private FirebaseSignupTransactionService transactionService;
	private AccessTokenIssuer accessTokenIssuer;
	private FirebaseSignupService service;
	private FirebaseEnrollmentAttempt attempt;
	private PreparedRefreshSession preparedSession;

	@BeforeEach
	void setUp() {
		verifier = mock(FirebaseAuthenticationVerifier.class);
		enrollmentRepository = mock(FirebaseEnrollmentAttemptRepository.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		socialIdentityRepository = mock(SocialIdentityRepository.class);
		aliasRepository = mock(PhoneFingerprintAliasRepository.class);
		refreshSessionIssuer = mock(RefreshSessionIssuer.class);
		transactionService = mock(FirebaseSignupTransactionService.class);
		accessTokenIssuer = mock(AccessTokenIssuer.class);

		ConsentPolicy consentPolicy = new ConsentPolicy("privacy-v1", "term-v1");
		UserFactory userFactory = new UserFactory(
				new EmailNormalizer(),
				mock(PasswordEncoder.class),
				consentPolicy
		);
		PhoneFingerprintHasher identityHasher = new PhoneFingerprintHasher(registry((byte) 1));
		PhoneEligibilityFingerprintHasher eligibilityHasher =
				new PhoneEligibilityFingerprintHasher("opaque-scope-v1", registry((byte) 2));
		service = new FirebaseSignupService(
				verifier,
				enrollmentRepository,
				firebaseIdentityRepository,
				socialIdentityRepository,
				aliasRepository,
				new PhoneNumberNormalizer(),
				identityHasher,
				eligibilityHasher,
				consentPolicy,
				userFactory,
				refreshSessionIssuer,
				transactionService,
				accessTokenIssuer,
				Clock.fixed(NOW, ZoneOffset.UTC)
		);

		attempt = directAttempt();
		when(verifier.verify("fresh-firebase-credential", FirebaseVerificationPurpose.DIRECT_ENROLLMENT))
				.thenReturn(principal(true, PHONE));
		when(enrollmentRepository.findById(attempt.getEnrollmentId()))
				.thenReturn(Optional.of(attempt));
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				PROJECT_ID,
				FIREBASE_UID
		)).thenReturn(Optional.empty());
		when(socialIdentityRepository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google-subject-sensitive"
		)).thenReturn(Optional.empty());
		when(aliasRepository.findAllActiveByFingerprints(any())).thenReturn(List.of());

		preparedSession = new PreparedRefreshSession(
				"refresh-secret",
				RefreshSession.create(
						"00000000-0000-4000-8000-000000000001",
						"refresh-hash",
						NOW,
						NOW.plus(Duration.ofDays(14))
				)
		);
		when(refreshSessionIssuer.prepare(any())).thenAnswer(invocation -> {
			String userId = invocation.getArgument(0);
			return new PreparedRefreshSession(
					"refresh-secret",
					RefreshSession.create(
							userId,
							"refresh-hash",
							NOW,
							NOW.plus(Duration.ofDays(14))
					)
			);
		});
		when(transactionService.register(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenReturn(new IssuedRefreshSession(
						"refresh-secret",
						NOW,
						NOW.plus(Duration.ofDays(14))
				));
		when(accessTokenIssuer.issue(any(), any())).thenReturn(new IssuedAccessToken(
				"access-secret",
				"Bearer",
				NOW,
				NOW.plus(Duration.ofMinutes(30)),
				1800
		));
	}

	@Test
	void createsCanonicalFederatedMemberAndIssuesAccessTokenAfterTransaction() {
		FirebaseSignupResponse response = service.signup(request());

		assertThat(response.accessToken()).isEqualTo("access-secret");
		assertThat(response.refreshToken()).isEqualTo("refresh-secret");
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		ArgumentCaptor<FirebaseIdentity> firebaseCaptor = ArgumentCaptor.forClass(
				FirebaseIdentity.class
		);
		verify(transactionService).register(
				userCaptor.capture(),
				firebaseCaptor.capture(),
				any(), any(), any(), any(), any(), any(), any()
		);
		User user = userCaptor.getValue();
		assertThat(user.getProvider()).isEqualTo(UserProvider.FEDERATED);
		assertThat(user.isMember()).isTrue();
		assertThat(user.hasLocalCredential()).isFalse();
		assertThat(user.getGuestInstallationIdHash()).isNull();
		assertThat(firebaseCaptor.getValue().getUserId()).isEqualTo(user.getUserId());

		InOrder order = inOrder(transactionService, accessTokenIssuer);
		order.verify(transactionService).register(any(), any(), any(), any(), any(), any(), any(), any(), any());
		order.verify(accessTokenIssuer).issue(user.getUserId(), Set.of());
		assertThat(response.toString())
				.doesNotContain("access-secret", "refresh-secret", FIREBASE_UID, PHONE);
	}

	@Test
	void rejectsMissingOrMismatchedEnrollmentBeforePreparingSession() {
		when(enrollmentRepository.findById(attempt.getEnrollmentId())).thenReturn(Optional.empty());

		assertAuthError(() -> service.signup(request()), AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);

		verify(refreshSessionIssuer, never()).prepare(any());
		verify(transactionService, never()).register(any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void rejectsMissingVerifiedPhoneWithoutCreatingUserOrSession() {
		when(verifier.verify(any(), any())).thenReturn(principal(false, null));

		assertAuthError(
				() -> service.signup(request()),
				AuthErrorStatus.FIREBASE_PHONE_VERIFICATION_REQUIRED
		);

		verify(refreshSessionIssuer, never()).prepare(any());
		verify(transactionService, never()).register(any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void duplicateFinalizeReturnsFixedEnrollmentConflictWithoutPreparingAnotherSession() {
		when(firebaseIdentityRepository.findByFirebaseProjectIdAndFirebaseUid(
				PROJECT_ID,
				FIREBASE_UID
		)).thenReturn(Optional.of(FirebaseIdentity.create(
				PROJECT_ID,
				FIREBASE_UID,
				"00000000-0000-4000-8000-000000000009",
				NOW
		)));

		assertAuthError(() -> service.signup(request()), AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);

		verify(refreshSessionIssuer, never()).prepare(any());
		verify(transactionService, never()).register(any(), any(), any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void uniqueRaceIsClassifiedAsPhoneConflictAndAccessTokenIsNotIssued() {
		when(transactionService.register(any(), any(), any(), any(), any(), any(), any(), any(), any()))
				.thenThrow(new DuplicateKeyException("test-only unique conflict"));
		when(aliasRepository.findAllActiveByFingerprints(any()))
				.thenReturn(List.of())
				.thenReturn(List.of(mock(web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias.class)));

		assertAuthError(() -> service.signup(request()), AuthErrorStatus.PHONE_ALREADY_LINKED);

		verify(accessTokenIssuer, never()).issue(any(), any());
	}

	@Test
	void requestAndPrincipalStringRepresentationsRedactSensitiveProofs() {
		assertThat(request().toString())
				.doesNotContain("fresh-firebase-credential", attempt.getEnrollmentId(), "토스회원");
		assertThat(principal(true, PHONE).toString())
				.doesNotContain(FIREBASE_UID, PROJECT_ID, PHONE, "google-subject-sensitive");
	}

	private FirebaseSignupRequest request() {
		return new FirebaseSignupRequest(
				attempt.getEnrollmentId(),
				"fresh-firebase-credential",
				"토스회원",
				true,
				"privacy-v1",
				true,
				"term-v1"
		);
	}

	private FirebaseEnrollmentAttempt directAttempt() {
		return FirebaseEnrollmentAttempt.create(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(60),
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}

	private VerifiedFirebasePrincipal principal(boolean phoneVerified, String phone) {
		return new VerifiedFirebasePrincipal(
				PROJECT_ID,
				FIREBASE_UID,
				FirebaseAuthenticationMethod.GOOGLE,
				NOW.minusSeconds(30),
				NOW.minusSeconds(20),
				NOW.plusSeconds(300),
				true,
				phoneVerified,
				phone,
				phoneVerified
						? Set.of(FirebaseAuthenticationMethod.GOOGLE, FirebaseAuthenticationMethod.PHONE)
						: Set.of(FirebaseAuthenticationMethod.GOOGLE),
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

	private void assertAuthError(Runnable invocation, AuthErrorStatus status) {
		assertThatThrownBy(invocation::run)
				.isInstanceOf(AuthException.class)
				.satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
						.isEqualTo(status));
	}
}
