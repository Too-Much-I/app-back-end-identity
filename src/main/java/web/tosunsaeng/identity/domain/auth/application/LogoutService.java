package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.auth.dto.request.LogoutRequest;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;

@Service
public class LogoutService {

	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final Clock clock;

	public LogoutService(
			RefreshTokenHasher refreshTokenHasher,
			RefreshSessionRepository refreshSessionRepository,
			Clock clock
	) {
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshSessionRepository = refreshSessionRepository;
		this.clock = clock;
	}

	public void logout(LogoutRequest request) {
		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		RefreshSession session = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElse(null);
		if (session == null) {
			return;
		}

		Instant currentTime = clock.instant();
		if (session.isRevoked() || session.isExpiredAt(currentTime)) {
			return;
		}

		session.logout(currentTime);
		refreshSessionRepository.save(session);
	}
}
