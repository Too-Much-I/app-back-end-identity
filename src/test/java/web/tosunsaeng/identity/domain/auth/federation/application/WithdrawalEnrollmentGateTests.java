package web.tosunsaeng.identity.domain.auth.federation.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

class WithdrawalEnrollmentGateTests {

	private static final String USER_ID = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";

	private UserRepository userRepository;
	private UserWithdrawalLifecycleRepository lifecycleRepository;
	private WithdrawalEnrollmentGate gate;
	private User user;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		lifecycleRepository = mock(UserWithdrawalLifecycleRepository.class);
		gate = new WithdrawalEnrollmentGate(userRepository, lifecycleRepository);
		user = mock(User.class);
		when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
	}

	@Test
	void activeOwnerPreservesExistingAuthenticationPolicy() {
		when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

		assertThatCode(() -> gate.checkExistingOwner(USER_ID)).doesNotThrowAnyException();
		verifyNoInteractions(lifecycleRepository);
	}

	@Test
	void suspendedOwnerAlsoLeavesExistingPathSpecificPolicyUntouched() {
		when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);

		assertThatCode(() -> gate.checkExistingOwner(USER_ID)).doesNotThrowAnyException();
		verifyNoInteractions(lifecycleRepository);
	}

	@Test
	void withdrawnOwnerBeforeCleanedUsesDedicatedPendingError() {
		when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		UserWithdrawalLifecycle lifecycle = lifecycle(
				UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING
		);
		when(lifecycleRepository.findByUserId(USER_ID)).thenReturn(Optional.of(lifecycle));

		assertThatThrownBy(() -> gate.checkExistingOwner(USER_ID))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
								.isEqualTo(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING));
	}

	@Test
	void cleanedOwnerWithStaleMappingFailsClosedAsInvariantConflict() {
		when(user.getStatus()).thenReturn(UserStatus.WITHDRAWN);
		UserWithdrawalLifecycle lifecycle = lifecycle(UserWithdrawalCleanupStatus.CLEANED);
		when(lifecycleRepository.findByUserId(USER_ID)).thenReturn(Optional.of(lifecycle));

		assertThatThrownBy(() -> gate.checkExistingOwner(USER_ID))
				.isInstanceOfSatisfying(AuthException.class, exception ->
						org.assertj.core.api.Assertions.assertThat(exception.getErrorCode())
								.isEqualTo(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT));
	}

	private UserWithdrawalLifecycle lifecycle(UserWithdrawalCleanupStatus status) {
		UserWithdrawalLifecycle lifecycle = mock(UserWithdrawalLifecycle.class);
		when(lifecycle.getStatus()).thenReturn(status);
		return lifecycle;
	}
}
