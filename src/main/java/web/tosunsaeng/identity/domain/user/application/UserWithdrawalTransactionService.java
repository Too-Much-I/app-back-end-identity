package web.tosunsaeng.identity.domain.user.application;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
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
@RequiredArgsConstructor
public class UserWithdrawalTransactionService {

	private final UserRepository userRepository;
	private final RefreshSessionRepository refreshSessionRepository;

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
