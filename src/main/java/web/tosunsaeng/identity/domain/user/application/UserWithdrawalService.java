package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.dto.request.WithdrawRequest;
import web.tosunsaeng.identity.domain.user.dto.response.WithdrawResponse;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

	private static final int MAX_WITHDRAWAL_ATTEMPTS = 2;
	private static final Logger log = LoggerFactory.getLogger(UserWithdrawalService.class);

	private final CurrentUserProvider currentUserProvider;
	private final UserRepository userRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshTokenHasher refreshTokenHasher;
	private final PasswordEncoder passwordEncoder;
	private final UserWithdrawalTransactionService transactionService;
	private final Clock clock;

	public WithdrawResponse withdraw(WithdrawRequest request) {
		String userId = currentUserProvider.getCurrentUserId();
		User user = findUser(userId);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			WithdrawResponse response = WithdrawResponse.from(user);
			logWithdrawalCompleted(user, response, "already_withdrawn", 0, 0);
			return response;
		}

		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		for (int attempt = 1; attempt <= MAX_WITHDRAWAL_ATTEMPTS; attempt++) {
			Instant withdrawnAt = clock.instant();
			validateCredentialSession(tokenHash, userId, withdrawnAt);
			validateProviderCredential(user, request.password());

			try {
				WithdrawalTransactionResult result = transactionService.withdraw(
						user,
						tokenHash,
						withdrawnAt
				);
				logWithdrawalCompleted(
						user,
						result.response(),
						"withdrawn",
						result.revokedSessionCount(),
						attempt
				);
				return result.response();
			} catch (UserException exception) {
				if (exception.getErrorCode() != UserErrorStatus.WITHDRAWAL_CONFLICT) {
					throw exception;
				}
				User latestUser = findUser(userId);
				if (latestUser.getStatus() == UserStatus.WITHDRAWN) {
					logWithdrawalConflict(userId, "concurrent_completion_detected", attempt);
					WithdrawResponse response = WithdrawResponse.from(latestUser);
					logWithdrawalCompleted(
							latestUser,
							response,
							"concurrent_idempotent",
							0,
							attempt
					);
					return response;
				}
				if (attempt == MAX_WITHDRAWAL_ATTEMPTS) {
					logWithdrawalConflict(userId, "rejected", attempt);
					throw exception;
				}
				logWithdrawalConflict(userId, "retrying", attempt);
				user = latestUser;
			}
		}
		throw new IllegalStateException("Withdrawal attempt limit must be positive.");
	}

	private void logWithdrawalCompleted(
			User user,
			WithdrawResponse response,
			String outcome,
			int revokedSessionCount,
			int attempt
	) {
		log.atInfo()
				.addKeyValue("event", "user.withdrawal.completed")
				.addKeyValue("outcome", outcome)
				.addKeyValue("userId", user.getUserId())
				.addKeyValue("accountType", user.getAccountType())
				.addKeyValue("withdrawnAt", response.withdrawnAt())
				.addKeyValue("revokedSessionCount", revokedSessionCount)
				.addKeyValue("attempt", attempt)
				.log("회원 탈퇴 처리가 완료되었습니다");
	}

	private void logWithdrawalConflict(String userId, String outcome, int attempt) {
		log.atWarn()
				.addKeyValue("event", "user.withdrawal.conflict")
				.addKeyValue("outcome", outcome)
				.addKeyValue("userId", userId)
				.addKeyValue("attempt", attempt)
				.addKeyValue("errorCode", UserErrorStatus.WITHDRAWAL_CONFLICT.getCode())
				.log("회원 탈퇴 처리 중 충돌이 발생했습니다");
	}

	private User findUser(String userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new UserException(UserErrorStatus.USER_NOT_FOUND));
	}

	private void validateCredentialSession(
			String tokenHash,
			String userId,
			Instant currentTime
	) {
		RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidCredentials);
		if (!session.getUserId().equals(userId)
				|| session.isRevoked()
				|| session.isExpiredAt(currentTime)) {
			throw invalidCredentials();
		}
	}

	private void validateProviderCredential(User user, String password) {
		if (user.isGuest()) {
			return;
		}
		if (!user.isMember() || !user.hasLocalCredential()) {
			throw invalidCredentials();
		}
		if (password == null || password.isBlank()) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_PASSWORD_REQUIRED);
		}
		if (user.getPasswordHash() == null
				|| !passwordEncoder.matches(password, user.getPasswordHash())) {
			throw invalidCredentials();
		}
	}

	private AuthException invalidCredentials() {
		return new AuthException(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
	}
}
