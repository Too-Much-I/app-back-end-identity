package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.session.dto.request.LogoutRequest;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

@Service
@RequiredArgsConstructor
public class LogoutService {

	private static final Logger log = LoggerFactory.getLogger(LogoutService.class);

	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final Clock clock;
	private ReissueRecoveryService recovery;
	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setRecovery(ReissueRecoveryService recovery) { this.recovery = recovery; }

	public void logout(LogoutRequest request) {
		if (recovery != null) { recovery.logout(request.refreshToken()); return; }
		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElse(null);
		// 존재하지 않거나 이미 종료된 세션도 로그아웃 성공으로 처리한다.
		if (session == null) {
			log.atDebug()
					.addKeyValue("event", "auth.logout.completed")
					.addKeyValue("outcome", "session_not_found")
					.addKeyValue("revokedSessionCount", 0)
					.log("변경할 세션 없이 로그아웃 요청을 처리했습니다");
			return;
		}

		Instant currentTime = clock.instant();
		if (session.getRotationResponseId() != null && session.getRecoveryDisabledAt() == null
				&& session.getRecoveryUntil() != null && currentTime.isBefore(session.getRecoveryUntil())) {
			throw SessionSecurityService.unavailable();
		}
		if (session.isRevoked() || session.isExpiredAt(currentTime)) {
			log.atDebug()
					.addKeyValue("event", "auth.logout.completed")
					.addKeyValue("outcome", session.isRevoked() ? "already_revoked" : "expired")
					.addKeyValue("userId", session.getUserId())
					.addKeyValue("revokedSessionCount", 0)
					.log("변경할 세션 없이 로그아웃 요청을 처리했습니다");
			return;
		}

		session.logout(currentTime);
		refreshSessionRepository.save(session);
		log.atInfo()
				.addKeyValue("event", "auth.logout.completed")
				.addKeyValue("outcome", "session_revoked")
				.addKeyValue("userId", session.getUserId())
				.addKeyValue("revokedSessionCount", 1)
				.log("로그아웃이 완료되었습니다");
	}
}
