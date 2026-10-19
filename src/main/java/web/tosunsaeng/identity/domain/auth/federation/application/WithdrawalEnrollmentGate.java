package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class WithdrawalEnrollmentGate {

	private final UserRepository userRepository;
	private final UserWithdrawalLifecycleRepository lifecycleRepository;

	public WithdrawalEnrollmentGate(
			UserRepository userRepository,
			UserWithdrawalLifecycleRepository lifecycleRepository
	) {
		this.userRepository = Objects.requireNonNull(userRepository);
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
	}

	public void checkExistingOwner(String ownerUserId) {
		String requiredOwnerId = Objects.requireNonNull(
				ownerUserId,
				"ownerUserId must not be null"
		);
		User owner = userRepository.findById(requiredOwnerId)
				.orElseThrow(this::invariantConflict);
		if (owner.getStatus() != UserStatus.WITHDRAWN) {
			return;
		}
		UserWithdrawalCleanupStatus status = lifecycleRepository.findByUserId(
					requiredOwnerId
			).map(lifecycle -> lifecycle.getStatus())
					.orElseThrow(this::invariantConflict);
		if (status != UserWithdrawalCleanupStatus.CLEANED) {
			throw new AuthException(AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING);
		}
		throw invariantConflict();
	}

	private AuthException invariantConflict() {
		return new AuthException(AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT);
	}
}
