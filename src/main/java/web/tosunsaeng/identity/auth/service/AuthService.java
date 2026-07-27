package web.tosunsaeng.identity.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.auth.dto.request.LogoutRequest;
import web.tosunsaeng.identity.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.security.refresh.IssuedRefreshSession;
import web.tosunsaeng.identity.security.refresh.RefreshSession;
import web.tosunsaeng.identity.security.refresh.RefreshSessionIssuer;
import web.tosunsaeng.identity.security.refresh.RefreshSessionRepository;
import web.tosunsaeng.identity.security.refresh.RefreshTokenHasher;
import web.tosunsaeng.identity.security.refresh.RevocationReason;
import web.tosunsaeng.identity.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserFactory;
import web.tosunsaeng.identity.user.domain.UserStatus;
import web.tosunsaeng.identity.user.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final UserFactory userFactory;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final RefreshTokenHasher refreshTokenHasher;
	private final RefreshSessionRepository refreshSessionRepository;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer,
			UserFactory userFactory,
			PasswordEncoder passwordEncoder,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			RefreshTokenHasher refreshTokenHasher,
			RefreshSessionRepository refreshSessionRepository,
			Clock clock
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
		this.userFactory = userFactory;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenIssuer = accessTokenIssuer;
		this.refreshSessionIssuer = refreshSessionIssuer;
		this.refreshTokenHasher = refreshTokenHasher;
		this.refreshSessionRepository = refreshSessionRepository;
		this.clock = clock;
	}

	public CheckEmailResponse checkEmail(String email) {
		String normalizedEmail = emailNormalizer.normalize(email);
		boolean exists = userRepository.existsByNormalizedEmail(normalizedEmail);
		return CheckEmailResponse.from(!exists);
	}

	public SignupResponse signup(SignupRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		if (userRepository.existsByNormalizedEmail(normalizedEmail)) {
			throw new BusinessException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}

		if (!Boolean.TRUE.equals(request.isAudioConsent())) {
			throw new BusinessException(AuthErrorStatus.AUDIO_CONSENT_REQUIRED);
		}

		User user = userFactory.create(
				request.email(),
				request.password(),
				request.nickname().trim()
		);

		try {
			User savedUser = userRepository.save(user);
			return SignupResponse.from(savedUser);
		} catch (DuplicateKeyException exception) {
			throw new BusinessException(AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		}
	}

	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		User user = userRepository.findByNormalizedEmail(normalizedEmail)
				.orElseThrow(() -> new BusinessException(AuthErrorStatus.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BusinessException(AuthErrorStatus.INVALID_CREDENTIALS);
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorStatus.ACCOUNT_NOT_ACTIVE);
		}

		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());
		IssuedRefreshSession refreshSession = refreshSessionIssuer.issue(user.getUserId());
		return LoginResponse.from(accessToken, refreshSession);
	}

	public ReissueResponse reissue(ReissueRequest request) {
		String tokenHash = refreshTokenHasher.hash(request.refreshToken());
		RefreshSession currentSession = refreshSessionRepository.findByTokenHash(tokenHash)
				.orElseThrow(this::invalidRefreshToken);
		Instant currentTime = clock.instant();

		if (currentSession.getRevocationReason() == RevocationReason.ROTATED) {
			revokeActiveSessionsForReuse(currentSession.getUserId(), currentTime);
			throw new BusinessException(AuthErrorStatus.REFRESH_TOKEN_REUSE_DETECTED);
		}
		if (currentSession.isRevoked()) {
			throw invalidRefreshToken();
		}
		if (currentSession.isExpiredAt(currentTime)) {
			throw new BusinessException(AuthErrorStatus.REFRESH_TOKEN_EXPIRED);
		}

		User user = userRepository.findById(currentSession.getUserId())
				.orElseThrow(this::invalidRefreshToken);
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(AuthErrorStatus.ACCOUNT_NOT_ACTIVE);
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
		return ReissueResponse.from(accessToken, refreshSession, currentTime);
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

	private BusinessException invalidRefreshToken() {
		return new BusinessException(AuthErrorStatus.INVALID_REFRESH_TOKEN);
	}
}
