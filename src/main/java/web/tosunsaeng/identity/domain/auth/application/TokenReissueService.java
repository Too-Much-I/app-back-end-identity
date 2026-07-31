package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class TokenReissueService {

	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final UserRepository userRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter authResponseConverter;
	private final Clock clock;

	public ReissueResponse reissue(ReissueRequest request) {
		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		RefreshSession currentSession = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidRefreshToken);
		Instant currentTime = clock.instant();

		// 회전된 토큰이 재사용되면 탈취 가능성에 대비해 활성 세션을 모두 폐기한다.
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
			// 낙관적 잠금으로 동일한 Refresh Token의 동시 회전을 차단한다.
			refreshSessionRepository.save(currentSession);
		} catch (OptimisticLockingFailureException exception) {
			throw invalidRefreshToken();
		}

		// 기존 세션 폐기가 저장된 후에만 후속 토큰과 세션을 발급한다.
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
