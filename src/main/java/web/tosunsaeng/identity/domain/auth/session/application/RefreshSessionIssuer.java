package web.tosunsaeng.identity.domain.auth.session.application;

import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
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

	private SessionSecurityService security;

	@Autowired(required = false)
	public void setSessionSecurity(SessionSecurityService security) { this.security = security; }
	public boolean isFenceEnabled() { return security != null; }
	public long captureEpoch(String userId) { return security == null ? 0 : security.captureEpoch(userId); }
	public SessionSecurityService security() { return security; }

	public IssuedRefreshSession issueAuthenticated(String userId, SessionAuthentication proof) {
		PreparedRefreshSession prepared = prepare(userId);
		prepared.session().attachAuthentication(proof);
		return savePrepared(prepared);
	}

	public IssuedRefreshSession issue(String userId) {
		return savePrepared(prepare(userId));
	}

	public PreparedRefreshSession prepare(String userId) {
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

	public IssuedRefreshSession savePrepared(PreparedRefreshSession preparedRefreshSession) {
		if (security != null) {
			return security.transaction(() -> {
				security.checkAndTouch(preparedRefreshSession.session(), true);
				return persistPrepared(preparedRefreshSession);
			});
		}
		return persistPrepared(preparedRefreshSession);
	}

	private IssuedRefreshSession persistPrepared(PreparedRefreshSession preparedRefreshSession) {
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
		if (security != null) throw SessionSecurityService.unavailable();
		return rotateWithEvidence(sessionId, userId, rotationFamilyId, rotatedFromSessionId, createdAt, null);
	}

	public IssuedRefreshSession issueRotated(RefreshSession previous, String sessionId, Instant createdAt) {
		return rotateWithEvidence(sessionId, previous.getUserId(), previous.getRotationFamilyId(),
				previous.getSessionId(), createdAt, previous.getAuthentication());
	}

	private IssuedRefreshSession rotateWithEvidence(String sessionId, String userId,
			String rotationFamilyId, String rotatedFromSessionId, Instant createdAt, SessionAuthentication proof) {
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

		if (proof != null) refreshSession.attachAuthentication(proof);
		if (security != null) security.checkAndTouch(refreshSession, false);
		RefreshSession savedSession = refreshSessionRepository.save(refreshSession);
		return new IssuedRefreshSession(
				tokenValue,
				savedSession.getCreatedAt(),
				savedSession.getExpiresAt()
		);
	}
}
