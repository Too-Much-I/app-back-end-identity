package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;

@Service
@RequiredArgsConstructor
public class LogoutAllService {

	private static final Logger log = LoggerFactory.getLogger(LogoutAllService.class);

	private final CurrentUserProvider currentUserProvider;
	private final RefreshSessionRepository refreshSessionRepository;
	private final Clock clock;

	public void logoutAll() {
		// 인증된 JWT의 사용자 ID로 본인이 소유한 세션만 조회한다.
		String userId = currentUserProvider.getCurrentUserId();
		List<RefreshSession> activeSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(userId);
		if (activeSessions.isEmpty()) {
			log.atDebug()
					.addKeyValue("event", "auth.logout_all.completed")
					.addKeyValue("outcome", "no_active_sessions")
					.addKeyValue("userId", userId)
					.addKeyValue("revokedSessionCount", 0)
					.log("Identity logout-all completed idempotently");
			return;
		}

		Instant currentTime = clock.instant();
		activeSessions.forEach(session -> session.logoutAll(currentTime));
		refreshSessionRepository.saveAll(activeSessions);
		log.atInfo()
				.addKeyValue("event", "auth.logout_all.completed")
				.addKeyValue("outcome", "sessions_revoked")
				.addKeyValue("userId", userId)
				.addKeyValue("revokedSessionCount", activeSessions.size())
				.log("Identity logout-all completed");
	}
}
