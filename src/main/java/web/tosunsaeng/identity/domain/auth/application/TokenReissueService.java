package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

@Service
public class TokenReissueService {

	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final UserRepository userRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter authResponseConverter;
	private final Clock clock;

	public TokenReissueService(
			RefreshTokenHasher refreshTokenHasher,
			RefreshSessionRepository refreshSessionRepository,
			UserRepository userRepository,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			AuthResponseConverter authResponseConverter,
			Clock clock
	) {
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshSessionRepository = refreshSessionRepository;
		this.userRepository = userRepository;
		this.accessTokenIssuer = accessTokenIssuer;
		this.refreshSessionIssuer = refreshSessionIssuer;
		this.authResponseConverter = authResponseConverter;
		this.clock = clock;
	}

	public ReissueResponse reissue(ReissueRequest request) {
		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		RefreshSession currentSession = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidRefreshToken);
		Instant currentTime = clock.instant();

		if (currentSession.getRevocationReason() == RevocationReason.ROTATED) {
			revokeActiveSessionsForReuse(currentSession.getUserId(), currentTime);
			throw new AuthException(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED);
		}
		if (currentSession.isRevoked()) {
			throw invalidRefreshToken();
		}
		if (currentSession.isExpiredAt(currentTime)) {
			throw new AuthException(AuthErrorStatus.REFRESH_TOKEN_EXPIRED);
		}

		User user = userRepository.findById(currentSession.getUserId())
				.orElseThrow(this::invalidRefreshToken);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}

		String rotationFamilyId = currentSession.initializeRotationFamilyIfMissing();
		String replacementSessionId = RefreshSession.newSessionId();
		currentSession.rotate(currentTime, replacementSessionId);
		try {
			refreshSessionRepository.save(currentSession);
		} catch (OptimisticLockingFailureException exception) {
			throw invalidRefreshToken();
		}

		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());
		IssuedRefreshSession refreshSession = refreshSessionIssuer.issueRotated(
				replacementSessionId,
				currentSession.getUserId(),
				rotationFamilyId,
				currentSession.getSessionId(),
				currentTime
		);
		return authResponseConverter.toReissueResponse(
				accessToken,
				refreshSession.tokenValue(),
				refreshSession.expiresAt(),
				currentTime
		);
	}

	private void revokeActiveSessionsForReuse(String userId, Instant currentTime) {
		List<RefreshSession> activeSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(userId)
				.stream()
				.filter(session -> !session.isRevoked())
				.toList();
		activeSessions.forEach(session -> session.revokeForReuse(currentTime));
		if (!activeSessions.isEmpty()) {
			refreshSessionRepository.saveAll(activeSessions);
		}
	}

	private AuthException invalidRefreshToken() {
		return new AuthException(AuthErrorStatus.INVALID_REFRESH_TOKEN);
	}
}
