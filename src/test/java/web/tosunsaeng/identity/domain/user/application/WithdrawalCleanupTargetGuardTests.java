package web.tosunsaeng.identity.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class WithdrawalCleanupTargetGuardTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final Instant NOW = Instant.parse("2026-08-25T04:00:00Z");

	private UserWithdrawalLifecycleRepository lifecycleRepository;
	private UserRepository userRepository;
	private FirebaseIdentityRepository firebaseIdentityRepository;
	private WithdrawalCleanupTargetGuard guard;
	private UserWithdrawalLifecycle claimed;

	@BeforeEach
	void setUp() {
		lifecycleRepository = mock(UserWithdrawalLifecycleRepository.class);
		userRepository = mock(UserRepository.class);
		firebaseIdentityRepository = mock(FirebaseIdentityRepository.class);
		guard = new WithdrawalCleanupTargetGuard(
				lifecycleRepository,
				userRepository,
				firebaseIdentityRepository
		);
		claimed = lifecycle("test-project", "opaque-uid");
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(claimed));
		User user = mock(User.class);
		when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
	}

	@Test
	void acceptsExactFirebaseIdentityTarget() {
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
				FirebaseIdentity.create("test-project", "opaque-uid", USER_ID, NOW)
		));

		WithdrawalCleanupTargetGuard.Result result = guard.verify(claimed, NOW);

		assertThat(result.type())
				.isEqualTo(WithdrawalCleanupTargetGuard.ResultType.FIREBASE_TARGET);
		assertThat(result.failureCode()).isNull();
	}

	@Test
	void recognizesCompleteNullTargetAsLocalWithoutFirebaseIdentity() {
		UserWithdrawalLifecycle local = lifecycle(null, null);
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(local));

		assertThat(guard.verify(local, NOW).type())
				.isEqualTo(WithdrawalCleanupTargetGuard.ResultType.LOCAL_TARGET);
	}

	@Test
	void rejectsOwnershipMismatchForReconciliation() {
		when(firebaseIdentityRepository.findByUserId(USER_ID)).thenReturn(Optional.of(
				FirebaseIdentity.create("test-project", "different-uid", USER_ID, NOW)
		));

		WithdrawalCleanupTargetGuard.Result result = guard.verify(claimed, NOW);

		assertThat(result.type())
				.isEqualTo(WithdrawalCleanupTargetGuard.ResultType.RECONCILIATION_REQUIRED);
		assertThat(result.failureCode())
				.isEqualTo(WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH);
	}

	@Test
	void treatsChangedVersionOrExpiredLeaseAsLeaseLost() {
		UserWithdrawalLifecycle current = lifecycle("test-project", "opaque-uid");
		when(current.getVersion()).thenReturn(8L);
		when(lifecycleRepository.findById("withdrawal-id")).thenReturn(Optional.of(current));

		assertThat(guard.verify(claimed, NOW).type())
				.isEqualTo(WithdrawalCleanupTargetGuard.ResultType.LEASE_LOST);

		when(current.getVersion()).thenReturn(7L);
		when(current.getLeaseUntil()).thenReturn(NOW);
		assertThat(guard.verify(claimed, NOW).type())
				.isEqualTo(WithdrawalCleanupTargetGuard.ResultType.LEASE_LOST);
	}

	private UserWithdrawalLifecycle lifecycle(String projectId, String uid) {
		UserWithdrawalLifecycle lifecycle = mock(UserWithdrawalLifecycle.class);
		when(lifecycle.getWithdrawalId()).thenReturn("withdrawal-id");
		when(lifecycle.getUserId()).thenReturn(USER_ID);
		when(lifecycle.getStatus())
				.thenReturn(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS);
		when(lifecycle.getFirebaseProjectId()).thenReturn(projectId);
		when(lifecycle.getFirebaseUid()).thenReturn(uid);
		when(lifecycle.getLeaseOwner()).thenReturn("lease-token");
		when(lifecycle.getLeaseUntil()).thenReturn(NOW.plusSeconds(30));
		when(lifecycle.getVersion()).thenReturn(7L);
		return lifecycle;
	}
}
