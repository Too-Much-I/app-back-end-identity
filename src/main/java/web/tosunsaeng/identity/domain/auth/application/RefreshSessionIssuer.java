package web.tosunsaeng.identity.domain.auth.application;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenGenerator;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.global.security.refresh.RefreshTokenProperties;

@Service
@RequiredArgsConstructor
public class RefreshSessionIssuer {

	private final RefreshTokenGenerator refreshTokenGenerator;
	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final RefreshTokenProperties properties;
	private final Clock clock;

	public IssuedRefreshSession issue(String userId) {
		return savePrepared(prepare(userId));
	}

	PreparedRefreshSession prepare(String userId) {
		String tokenValue = refreshTokenGenerator.generate();
		// DB에는 Refresh Token 원문 대신 조회용 해시만 저장한다.
		String tokenHash = refreshTokenHasher.hash(tokenValue);
		Instant createdAt = clock.instant();
		RefreshSession refreshSession = RefreshSession.create(
				userId,
				tokenHash,
				createdAt,
				createdAt.plus(properties.ttl())
		);

		return new PreparedRefreshSession(tokenValue, refreshSession);
	}

	IssuedRefreshSession savePrepared(PreparedRefreshSession preparedRefreshSession) {
		RefreshSession savedSession = refreshSessionRepository.save(
				preparedRefreshSession.session()
		);
		return new IssuedRefreshSession(
				preparedRefreshSession.tokenValue(),
				savedSession.getCreatedAt(),
				savedSession.getExpiresAt()
		);
	}

	public IssuedRefreshSession issueRotated(
			String sessionId,
			String userId,
			String rotationFamilyId,
			String rotatedFromSessionId,
			Instant createdAt
	) {
		String tokenValue = refreshTokenGenerator.generate();
		String tokenHash = refreshTokenHasher.hash(tokenValue);
		RefreshSession refreshSession = RefreshSession.createRotated(
				sessionId,
				userId,
				rotationFamilyId,
				rotatedFromSessionId,
				tokenHash,
				createdAt,
				createdAt.plus(properties.ttl())
		);

		RefreshSession savedSession = refreshSessionRepository.save(refreshSession);
		return new IssuedRefreshSession(
				tokenValue,
				savedSession.getCreatedAt(),
				savedSession.getExpiresAt()
		);
	}
}
