package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.enums.RevocationReason;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
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

	private static final Logger log = LoggerFactory.getLogger(TokenReissueService.class);

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
			int revokedSessionCount = revokeActiveSessionsForReuse(
					currentSession.getUserId(),
					currentTime
			);
			log.atWarn()
					.addKeyValue("event", "auth.refresh.reuse_detected")
					.addKeyValue("outcome", "active_sessions_revoked")
					.addKeyValue("userId", currentSession.getUserId())
					.addKeyValue("revokedSessionCount", revokedSessionCount)
					.addKeyValue("errorCode", AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED.getCode())
					.log("Refresh Token 재사용을 감지했습니다");
			throw new AuthException(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED);
		}
		if (currentSession.getRevocationReason() == RevocationReason.ACCOUNT_WITHDRAWN) {
			throw new AuthException(AuthErrorStatus.ACCOUNT_WITHDRAWN);
		}
		if (currentSession.isRevoked()) {
			throw invalidRefreshToken();
		}
		if (currentSession.isExpiredAt(currentTime)) {
			throw new AuthException(AuthErrorStatus.REFRESH_TOKEN_EXPIRED);
		}

		User user = userRepository.findById(currentSession.getUserId())
				.orElseThrow(this::invalidRefreshToken);
		if (user.getStatus() == UserStatus.WITHDRAWN) {
			throw new AuthException(AuthErrorStatus.ACCOUNT_WITHDRAWN);
		}
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
			log.atInfo()
					.addKeyValue("event", "auth.refresh.reissue.rejected")
					.addKeyValue("outcome", "concurrent_rotation_rejected")
					.addKeyValue("userId", currentSession.getUserId())
					.addKeyValue("errorCode", AuthErrorStatus.INVALID_REFRESH_TOKEN.getCode())
					.log("동시에 요청된 Refresh Token 재발급을 거절했습니다");
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
		ReissueResponse response = authResponseConverter.toReissueResponse(
				accessToken,
				refreshSession.tokenValue(),
				refreshSession.expiresAt(),
				currentTime
		);
		log.atInfo()
				.addKeyValue("event", "auth.refresh.reissued")
				.addKeyValue("outcome", "rotated")
				.addKeyValue("userId", currentSession.getUserId())
				.log("Refresh Token을 재발급했습니다");
		return response;
	}

	private int revokeActiveSessionsForReuse(String userId, Instant currentTime) {
		List<RefreshSession> activeSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(userId)
				.stream()
				.filter(session -> !session.isRevoked())
				.toList();
		activeSessions.forEach(session -> session.revokeForReuse(currentTime));
		if (!activeSessions.isEmpty()) {
			refreshSessionRepository.saveAll(activeSessions);
		}
		return activeSessions.size();
	}

	private AuthException invalidRefreshToken() {
		return new AuthException(AuthErrorStatus.INVALID_REFRESH_TOKEN);
	}
}
