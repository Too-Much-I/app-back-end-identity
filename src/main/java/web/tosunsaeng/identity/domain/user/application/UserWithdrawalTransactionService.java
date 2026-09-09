package web.tosunsaeng.identity.domain.user.application;

import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingOutboxRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneEligibilityBindingRevisionRepository;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawnOutboxRepository;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

@Service
public class UserWithdrawalTransactionService {
	private SessionSecurityService sessionSecurity;
	@Autowired(required = false)
	public void setSessionSecurity(SessionSecurityService security) { sessionSecurity = security; }

	private final UserRepository userRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final PhoneEligibilityBindingRevisionRepository bindingRevisionRepository;
	private final PhoneEligibilityBindingOutboxRepository bindingOutboxRepository;
	private final UserWithdrawalLifecycleRepository lifecycleRepository;
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final UserWithdrawnOutboxRepository userWithdrawnOutboxRepository;

	@Autowired
	public UserWithdrawalTransactionService(
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			@Nullable PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			@Nullable PhoneEligibilityBindingOutboxRepository bindingOutboxRepository,
			@Nullable UserWithdrawalLifecycleRepository lifecycleRepository,
			@Nullable FirebaseIdentityRepository firebaseIdentityRepository,
			UserWithdrawnOutboxRepository userWithdrawnOutboxRepository
	) {
		this.userRepository = userRepository;
		this.refreshSessionRepository = refreshSessionRepository;
		this.bindingRevisionRepository = bindingRevisionRepository;
		this.bindingOutboxRepository = bindingOutboxRepository;
		this.lifecycleRepository = lifecycleRepository;
		this.firebaseIdentityRepository = firebaseIdentityRepository;
		this.userWithdrawnOutboxRepository = userWithdrawnOutboxRepository;
	}

	public UserWithdrawalTransactionService(
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			@Nullable PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			@Nullable PhoneEligibilityBindingOutboxRepository bindingOutboxRepository,
			UserWithdrawnOutboxRepository userWithdrawnOutboxRepository
	) {
		this(userRepository, refreshSessionRepository, bindingRevisionRepository,
				bindingOutboxRepository, null, null, userWithdrawnOutboxRepository);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public WithdrawalTransactionResult withdraw(
			User currentUser,
			String credentialTokenHash,
			Instant withdrawnAt,
			@Nullable FirebaseWithdrawalTarget firebaseTarget
	) {
		validateCredentialSession(
				credentialTokenHash,
				currentUser.getUserId(),
				withdrawnAt
		);
		if (sessionSecurity != null) sessionSecurity.handoffWithdrawal(currentUser.getUserId(), withdrawnAt);

		UserStatus expectedStatus = currentUser.getStatus();
		Instant expectedUpdatedAt = currentUser.getUpdatedAt();
		User tombstone = currentUser.toWithdrawnTombstone(withdrawnAt);
		boolean userUpdated = userRepository.withdrawIfUnchanged(
				tombstone,
				expectedStatus,
				expectedUpdatedAt
		);
		if (!userUpdated) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_CONFLICT);
		}

		validateFirebaseTarget(currentUser.getUserId(), firebaseTarget);
		createLifecycle(currentUser.getUserId(), firebaseTarget, withdrawnAt);

		publishBindingRevocations(currentUser.getUserId(), withdrawnAt);

		List<RefreshSession> activeSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(currentUser.getUserId())
				.stream()
				.filter(session -> !session.isRevoked())
				.toList();
		activeSessions.forEach(session -> session.withdrawAccount(withdrawnAt));
		if (!activeSessions.isEmpty()) {
			try {
				refreshSessionRepository.saveAll(activeSessions);
			} catch (OptimisticLockingFailureException exception) {
				throw new UserException(UserErrorStatus.WITHDRAWAL_CONFLICT);
			}
		}

		userWithdrawnOutboxRepository.save(UserWithdrawnOutbox.create(
				currentUser.getUserId(),
				withdrawnAt
		));

		return new WithdrawalTransactionResult(
				WithdrawResponse.from(tombstone),
				activeSessions.size()
		);
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public WithdrawalTransactionResult withdraw(
			User currentUser,
			String credentialTokenHash,
			Instant withdrawnAt
	) {
		return withdraw(currentUser, credentialTokenHash, withdrawnAt, null);
	}

	private void validateFirebaseTarget(
			String userId,
			@Nullable FirebaseWithdrawalTarget firebaseTarget
	) {
		if (firebaseTarget == null) {
			return;
		}
		if (firebaseIdentityRepository == null) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_LIFECYCLE_CONFLICT);
		}
		boolean matches = firebaseIdentityRepository
				.findByFirebaseProjectIdAndFirebaseUid(
						firebaseTarget.firebaseProjectId(), firebaseTarget.firebaseUid()
				)
				.map(identity -> identity.getUserId().equals(userId))
				.orElse(false);
		if (!matches) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_LIFECYCLE_CONFLICT);
		}
	}

	private void createLifecycle(
			String userId,
			@Nullable FirebaseWithdrawalTarget firebaseTarget,
			Instant withdrawnAt
	) {
		if (lifecycleRepository == null) {
			return;
		}
		if (lifecycleRepository.findByUserId(userId).isPresent()) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_LIFECYCLE_CONFLICT);
		}
		lifecycleRepository.save(UserWithdrawalLifecycle.create(
				userId,
				firebaseTarget == null ? null : firebaseTarget.firebaseProjectId(),
				firebaseTarget == null ? null : firebaseTarget.firebaseUid(),
				withdrawnAt
		));
	}

	private void publishBindingRevocations(String userId, Instant withdrawnAt) {
		if (bindingRevisionRepository == null && bindingOutboxRepository == null) {
			return;
		}
		if (bindingRevisionRepository == null || bindingOutboxRepository == null) {
			throw new IllegalStateException("Phone eligibility binding repositories are incomplete.");
		}
		List<PhoneEligibilityBindingRevision> activeBindings = bindingRevisionRepository
				.findAllByUserIdAndActiveTrue(userId);
		for (PhoneEligibilityBindingRevision activeBinding : activeBindings) {
			PhoneEligibilityBindingRevision revokedBinding = bindingRevisionRepository.advanceRevoked(
					activeBinding.getUserId(), activeBinding.getConsumerScopeId(),
					activeBinding.getRevision(), withdrawnAt
			).orElseThrow(() -> new UserException(UserErrorStatus.WITHDRAWAL_CONFLICT));
			bindingOutboxRepository.save(PhoneEligibilityBindingOutbox.createRevoked(
					revokedBinding.getUserId(), revokedBinding.getConsumerScopeId(),
					revokedBinding.getRevision(), withdrawnAt, withdrawnAt));
		}
	}

	private void validateCredentialSession(
			String credentialTokenHash,
			String userId,
			Instant currentTime
	) {
		RefreshSession session = refreshSessionRepository
				.findByTokenHash(credentialTokenHash)
				.orElseThrow(this::invalidCredentials);
		if (sessionSecurity != null) sessionSecurity.checkAndTouch(session, false);
		if (!session.getUserId().equals(userId)
				|| session.isRevoked()
				|| session.isExpiredAt(currentTime)) {
			throw invalidCredentials();
		}
	}

	private AuthException invalidCredentials() {
		return new AuthException(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
	}
}
