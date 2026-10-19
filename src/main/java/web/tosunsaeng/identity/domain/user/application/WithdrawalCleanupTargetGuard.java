package web.tosunsaeng.identity.domain.user.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;

public final class WithdrawalCleanupTargetGuard {
	private SessionSecurityService sessionSecurity;
	@Autowired(required = false)
	public void setSessionSecurity(SessionSecurityService security) { sessionSecurity = security; }

	public enum ResultType { LOCAL_TARGET, FIREBASE_TARGET, LEASE_LOST, LOGOUT_PENDING, RECONCILIATION_REQUIRED }

	public record Result(ResultType type, WithdrawalCleanupFailureCode failureCode) {
		public Result {
			Objects.requireNonNull(type, "type must not be null");
			if ((type == ResultType.RECONCILIATION_REQUIRED) != (failureCode != null)) {
				throw new IllegalArgumentException("failureCode must exist only for reconciliation");
			}
		}
	}

	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final UserRepository userRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;

	public WithdrawalCleanupTargetGuard(
			UserWithdrawalLifecycleRepository lifecycleRepository,
			UserRepository userRepository,
			FirebaseIdentityRepository firebaseIdentityRepository
	) {
		this.lifecycleRepository = Objects.requireNonNull(lifecycleRepository);
		this.userRepository = Objects.requireNonNull(userRepository);
		this.firebaseIdentityRepository = Objects.requireNonNull(firebaseIdentityRepository);
	}

	public Result verify(UserWithdrawalLifecycle claimed, Instant now) {
		UserWithdrawalLifecycle requiredClaim = Objects.requireNonNull(claimed, "claimed must not be null");
		Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
		Optional<UserWithdrawalLifecycle> current = lifecycleRepository.findById(
				requiredClaim.getWithdrawalId()
		);
		if (current.isEmpty() || !sameClaim(requiredClaim, current.orElseThrow(), requiredNow)) {
			return new Result(ResultType.LEASE_LOST, null);
		}

		Optional<User> user = userRepository.findById(requiredClaim.getUserId());
		if (user.isEmpty() || user.orElseThrow().getStatus() != UserStatus.WITHDRAWN) {
			return reconciliation(WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH);
		}
		if (sessionSecurity != null && sessionSecurity.hasUnresolvedLogout(requiredClaim.getUserId())) {
			return new Result(ResultType.LOGOUT_PENDING, null);
		}

		boolean projectMissing = requiredClaim.getFirebaseProjectId() == null;
		boolean uidMissing = requiredClaim.getFirebaseUid() == null;
		if (projectMissing && uidMissing) {
			return new Result(ResultType.LOCAL_TARGET, null);
		}
		if (projectMissing != uidMissing) {
			return reconciliation(WithdrawalCleanupFailureCode.INVARIANT_VIOLATION);
		}

		Optional<FirebaseIdentity> identity = firebaseIdentityRepository.findByUserId(
				requiredClaim.getUserId()
		);
		if (identity.isEmpty()) {
			return reconciliation(WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH);
		}
		FirebaseIdentity existing = identity.orElseThrow();
		if (!requiredClaim.getUserId().equals(existing.getUserId())
				|| !requiredClaim.getFirebaseProjectId().equals(existing.getFirebaseProjectId())
				|| !requiredClaim.getFirebaseUid().equals(existing.getFirebaseUid())) {
			return reconciliation(WithdrawalCleanupFailureCode.TARGET_OWNERSHIP_MISMATCH);
		}
		return new Result(ResultType.FIREBASE_TARGET, null);
	}

	private boolean sameClaim(
			UserWithdrawalLifecycle claimed,
			UserWithdrawalLifecycle current,
			Instant now
	) {
		return current.getStatus() == UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS
				&& Objects.equals(current.getLeaseOwner(), claimed.getLeaseOwner())
				&& Objects.equals(current.getVersion(), claimed.getVersion())
				&& current.getLeaseUntil() != null
				&& current.getLeaseUntil().isAfter(now);
	}

	private static Result reconciliation(WithdrawalCleanupFailureCode code) {
		return new Result(ResultType.RECONCILIATION_REQUIRED, code);
	}
}
