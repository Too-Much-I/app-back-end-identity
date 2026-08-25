package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;
import web.tosunsaeng.identity.domain.user.dto.request.WithdrawRequest;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

class FirebaseUserWithdrawalServiceTests {

	private static final Instant NOW = Instant.parse("2026-08-25T01:02:03Z");
	private static final String REFRESH = "opaque-refresh";
	private static final String FIREBASE_PROOF = "fresh-firebase-proof";

	private User user;
	private FirebaseIdentity identity;
	private FirebaseWithdrawalCredentialVerifier firebaseVerifier;
	private UserWithdrawalTransactionService transactionService;
	private UserWithdrawalService service;

	@BeforeEach
	void setUp() {
		user = User.createFederatedMember(
				"SNS회원", UserConsents.consented("privacy-v1", "term-v1", NOW), NOW
		);
		identity = FirebaseIdentity.create(
				"firebase-project", "firebase-uid", user.getUserId(), NOW
		);
		CurrentUserProvider currentUserProvider = mock(CurrentUserProvider.class);
		UserRepository userRepository = mock(UserRepository.class);
		RefreshSessionRepository sessionRepository = mock(RefreshSessionRepository.class);
		FirebaseIdentityRepository identityRepository = mock(FirebaseIdentityRepository.class);
		firebaseVerifier = mock(FirebaseWithdrawalCredentialVerifier.class);
		transactionService = mock(UserWithdrawalTransactionService.class);
		UserWithdrawalLifecycleRepository lifecycleRepository = mock(
				UserWithdrawalLifecycleRepository.class
		);
		RefreshTokenHasher hasher = new RefreshTokenHasher();
		when(currentUserProvider.getCurrentUserId()).thenReturn(user.getUserId());
		when(userRepository.findById(user.getUserId())).thenReturn(Optional.of(user));
		when(identityRepository.findByUserId(user.getUserId())).thenReturn(Optional.of(identity));
		when(sessionRepository.findByTokenHash(hasher.hash(REFRESH))).thenReturn(Optional.of(
				RefreshSession.create(
						user.getUserId(), hasher.hash(REFRESH), NOW, NOW.plusSeconds(600)
				)
		));
		service = new UserWithdrawalService(
				currentUserProvider, userRepository, sessionRepository, hasher,
				new BCryptPasswordEncoder(4), transactionService,
				Clock.fixed(NOW, ZoneOffset.UTC), identityRepository, firebaseVerifier,
				lifecycleRepository
		);
	}

	@Test
	void federatedMemberWithdrawsWithFreshFirebaseProof() {
		FirebaseWithdrawalTarget target = new FirebaseWithdrawalTarget(
				"firebase-project", "firebase-uid"
		);
		User tombstone = user.toWithdrawnTombstone(NOW);
		when(firebaseVerifier.verify(user.getUserId(), identity, FIREBASE_PROOF))
				.thenReturn(target);
		when(transactionService.withdraw(eq(user), any(), eq(NOW), eq(target)))
				.thenReturn(new WithdrawalTransactionResult(WithdrawResponse.from(tombstone), 1));

		WithdrawResponse response = service.withdraw(
				new WithdrawRequest(REFRESH, null, FIREBASE_PROOF)
		);

		assertThat(response.cleanupStatus().name()).isEqualTo("EXTERNAL_CLEANUP_PENDING");
		verify(firebaseVerifier).verify(user.getUserId(), identity, FIREBASE_PROOF);
		verify(transactionService).withdraw(eq(user), any(), eq(NOW), eq(target));
	}

	@Test
	void federatedMemberRequiresFirebaseProofAndRejectsPasswordMixing() {
		BusinessException missing = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH, null, null))
		);
		BusinessException mixed = catchThrowableOfType(
				BusinessException.class,
				() -> service.withdraw(new WithdrawRequest(REFRESH, "password", FIREBASE_PROOF))
		);

		assertThat(missing.getErrorCode())
				.isEqualTo(AuthErrorStatus.WITHDRAWAL_FIREBASE_PROOF_REQUIRED);
		assertThat(mixed.getErrorCode())
				.isEqualTo(AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH);
	}
}
