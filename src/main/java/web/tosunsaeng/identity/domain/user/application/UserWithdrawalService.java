package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;
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
			return WithdrawResponse.from(user);
		}

		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		for (int attempt = 1; attempt <= MAX_WITHDRAWAL_ATTEMPTS; attempt++) {
			Instant withdrawnAt = clock.instant();
			validateCredentialSession(tokenHash, userId, withdrawnAt);
			validateProviderCredential(user, request.password());

			try {
				return transactionService.withdraw(user, tokenHash, withdrawnAt);
			} catch (UserException exception) {
				if (exception.getErrorCode() != UserErrorStatus.WITHDRAWAL_CONFLICT) {
					throw exception;
				}
				User latestUser = findUser(userId);
				if (latestUser.getStatus() == UserStatus.WITHDRAWN) {
					return WithdrawResponse.from(latestUser);
				}
				if (attempt == MAX_WITHDRAWAL_ATTEMPTS) {
					throw exception;
				}
				user = latestUser;
			}
		}
		throw new IllegalStateException("Withdrawal attempt limit must be positive.");
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
		if (user.getProvider() == UserProvider.GUEST) {
			return;
		}
		if (user.getProvider() != UserProvider.LOCAL) {
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
