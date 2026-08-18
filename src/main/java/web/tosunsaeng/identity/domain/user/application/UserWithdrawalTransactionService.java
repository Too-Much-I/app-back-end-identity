package web.tosunsaeng.identity.domain.user.application;

import java.time.Instant;
import java.util.List;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

@Service
public class UserWithdrawalTransactionService {

	private final UserRepository userRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final PhoneEligibilityBindingRevisionRepository bindingRevisionRepository;
	private final PhoneEligibilityBindingOutboxRepository bindingOutboxRepository;

	public UserWithdrawalTransactionService(
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			@Nullable PhoneEligibilityBindingRevisionRepository bindingRevisionRepository,
			@Nullable PhoneEligibilityBindingOutboxRepository bindingOutboxRepository
	) {
		this.userRepository = userRepository;
		this.refreshSessionRepository = refreshSessionRepository;
		this.bindingRevisionRepository = bindingRevisionRepository;
		this.bindingOutboxRepository = bindingOutboxRepository;
	}

	@Transactional(transactionManager = "mongoTransactionManager")
	public WithdrawalTransactionResult withdraw(
			User currentUser,
			String credentialTokenHash,
			Instant withdrawnAt
	) {
		validateCredentialSession(
				credentialTokenHash,
				currentUser.getUserId(),
				withdrawnAt
		);

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

		return new WithdrawalTransactionResult(
				WithdrawResponse.from(tombstone),
				activeSessions.size()
		);
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
