package web.tosunsaeng.identity.auth.service;

import java.util.Set;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.security.jwt.IssuedAccessToken;
import web.tosunsaeng.identity.security.refresh.IssuedRefreshSession;
import web.tosunsaeng.identity.security.refresh.RefreshSessionIssuer;
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

	public AuthService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer,
			UserFactory userFactory,
			PasswordEncoder passwordEncoder,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
		this.userFactory = userFactory;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenIssuer = accessTokenIssuer;
		this.refreshSessionIssuer = refreshSessionIssuer;
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
}
