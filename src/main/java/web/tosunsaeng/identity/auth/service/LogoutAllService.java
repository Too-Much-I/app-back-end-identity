package web.tosunsaeng.identity.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.security.currentuser.CurrentUserProvider;
import web.tosunsaeng.identity.security.refresh.RefreshSession;
import web.tosunsaeng.identity.security.refresh.RefreshSessionRepository;

@Service
public class LogoutAllService {

	private final CurrentUserProvider currentUserProvider;
	private final RefreshSessionRepository refreshSessionRepository;
	private final Clock clock;

	public LogoutAllService(
			CurrentUserProvider currentUserProvider,
			RefreshSessionRepository refreshSessionRepository,
			Clock clock
	) {
		this.currentUserProvider = currentUserProvider;
		this.refreshSessionRepository = refreshSessionRepository;
		this.clock = clock;
	}

	public void logoutAll() {
		String userId = currentUserProvider.getCurrentUserId();
		List<RefreshSession> activeSessions = refreshSessionRepository
				.findAllByUserIdAndRevokedAtIsNull(userId);
		if (activeSessions.isEmpty()) {
			return;
		}

		Instant currentTime = clock.instant();
		activeSessions.forEach(session -> session.logoutAll(currentTime));
		refreshSessionRepository.saveAll(activeSessions);
	}
}
