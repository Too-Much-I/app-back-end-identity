package web.tosunsaeng.identity.domain.user.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.lang.Nullable;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserWithdrawalLifecycleRepository;
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
	private final FirebaseIdentityRepository firebaseIdentityRepository;
	private final FirebaseWithdrawalCredentialVerifier firebaseWithdrawalCredentialVerifier;
	private final UserWithdrawalLifecycleRepository lifecycleRepository;

	@Autowired
	public UserWithdrawalService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			RefreshTokenHasher refreshTokenHasher,
			PasswordEncoder passwordEncoder,
			UserWithdrawalTransactionService transactionService,
			Clock clock,
			@Nullable FirebaseIdentityRepository firebaseIdentityRepository,
			FirebaseWithdrawalCredentialVerifier firebaseWithdrawalCredentialVerifier,
			@Nullable UserWithdrawalLifecycleRepository lifecycleRepository
	) {
		this.currentUserProvider = currentUserProvider;
		this.userRepository = userRepository;
		this.refreshSessionRepository = refreshSessionRepository;
		this.refreshTokenHasher = refreshTokenHasher;
		this.passwordEncoder = passwordEncoder;
		this.transactionService = transactionService;
		this.clock = clock;
		this.firebaseIdentityRepository = firebaseIdentityRepository;
		this.firebaseWithdrawalCredentialVerifier = firebaseWithdrawalCredentialVerifier;
		this.lifecycleRepository = lifecycleRepository;
	}

	public UserWithdrawalService(
			CurrentUserProvider currentUserProvider,
			UserRepository userRepository,
			RefreshSessionRepository refreshSessionRepository,
			RefreshTokenHasher refreshTokenHasher,
			PasswordEncoder passwordEncoder,
			UserWithdrawalTransactionService transactionService,
			Clock clock
	) {
		this(currentUserProvider, userRepository, refreshSessionRepository, refreshTokenHasher,
				passwordEncoder, transactionService, clock, null, null, null);
	}

	public WithdrawResponse withdraw(WithdrawRequest request) {
		String userId = currentUserProvider.getCurrentUserId();
		User user = findUser(userId);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			WithdrawResponse response = existingLifecycleResponse(user);
			logWithdrawalCompleted(user, response, "already_withdrawn", 0, 0);
			return response;
		}

		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		for (int attempt = 1; attempt <= MAX_WITHDRAWAL_ATTEMPTS; attempt++) {
			Instant withdrawnAt = clock.instant();
			validateCredentialSession(tokenHash, userId, withdrawnAt);
			FirebaseWithdrawalTarget firebaseTarget = validateProviderCredential(user, request);

			try {
				WithdrawalTransactionResult result = lifecycleRepository == null
						? transactionService.withdraw(user, tokenHash, withdrawnAt)
						: transactionService.withdraw(
								user, tokenHash, withdrawnAt, firebaseTarget
						);
				logWithdrawalCompleted(
						user,
						result.response(),
						"withdrawn",
						result.revokedSessionCount(),
						attempt
				);
				return result.response();
			} catch (DuplicateKeyException exception) {
				return resolveConcurrentCompletion(userId, user, attempt);
			} catch (UserException exception) {
				if (exception.getErrorCode() != UserErrorStatus.WITHDRAWAL_CONFLICT) {
					throw exception;
				}
				User latestUser = findUser(userId);
				if (latestUser.getStatus() == UserStatus.WITHDRAWN) {
					WithdrawResponse response = existingLifecycleResponse(latestUser);
					logWithdrawalConflict(userId, "concurrent_completion_detected", attempt);
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

	private WithdrawResponse resolveConcurrentCompletion(
			String userId,
			User originalUser,
			int attempt
	) {
		User latestUser = findUser(userId);
		if (latestUser.getStatus() != UserStatus.WITHDRAWN) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_CONFLICT);
		}
		WithdrawResponse response = existingLifecycleResponse(latestUser);
		logWithdrawalConflict(userId, "concurrent_lifecycle_completion", attempt);
		logWithdrawalCompleted(originalUser, response, "concurrent_idempotent", 0, attempt);
		return response;
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

	private FirebaseWithdrawalTarget validateProviderCredential(
			User user,
			WithdrawRequest request
	) {
		boolean hasPassword = request.password() != null && !request.password().isBlank();
		boolean hasFirebaseProof = request.firebaseIdToken() != null
				&& !request.firebaseIdToken().isBlank();
		if (user.isGuest()) {
			if (hasPassword || hasFirebaseProof) {
				throw new AuthException(AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH);
			}
			return null;
		}
		FirebaseIdentity firebaseIdentity = firebaseIdentityRepository == null
				? null
				: firebaseIdentityRepository.findByUserId(user.getUserId()).orElse(null);
		boolean hasLocalCredential = user.hasLocalCredential();
		if (firebaseIdentityRepository == null && !hasLocalCredential) {
			throw invalidCredentials();
		}
		if (!user.isMember() || (firebaseIdentity != null && hasLocalCredential)
				|| (firebaseIdentity == null && !hasLocalCredential)) {
			throw new AuthException(AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH);
		}
		if (firebaseIdentity != null) {
			if (hasPassword) {
				throw new AuthException(AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH);
			}
			if (!hasFirebaseProof) {
				throw new AuthException(AuthErrorStatus.WITHDRAWAL_FIREBASE_PROOF_REQUIRED);
			}
			return firebaseWithdrawalCredentialVerifier.verify(
					user.getUserId(), firebaseIdentity, request.firebaseIdToken()
			);
		}
		if (hasFirebaseProof) {
			throw new AuthException(AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH);
		}
		if (!hasPassword) {
			throw new UserException(UserErrorStatus.WITHDRAWAL_PASSWORD_REQUIRED);
		}
		if (user.getPasswordHash() == null
				|| !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}
		return null;
	}

	private WithdrawResponse existingLifecycleResponse(User user) {
		if (lifecycleRepository == null) {
			return WithdrawResponse.from(user);
		}
		return lifecycleRepository.findByUserId(user.getUserId())
				.map(lifecycle -> WithdrawResponse.from(user, lifecycle.getStatus()))
				.orElseThrow(() -> new UserException(
						UserErrorStatus.WITHDRAWAL_LIFECYCLE_CONFLICT
				));
	}

	private AuthException invalidCredentials() {
		return new AuthException(AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS);
	}
}
