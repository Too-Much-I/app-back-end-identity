package web.tosunsaeng.identity.domain.auth.application;

import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Service
public class LoginService {

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter authResponseConverter;

	public LoginService(
			UserRepository userRepository,
			EmailNormalizer emailNormalizer,
			PasswordEncoder passwordEncoder,
			AccessTokenIssuer accessTokenIssuer,
			RefreshSessionIssuer refreshSessionIssuer,
			AuthResponseConverter authResponseConverter
	) {
		this.userRepository = userRepository;
		this.emailNormalizer = emailNormalizer;
		this.passwordEncoder = passwordEncoder;
		this.accessTokenIssuer = accessTokenIssuer;
		this.refreshSessionIssuer = refreshSessionIssuer;
		this.authResponseConverter = authResponseConverter;
	}

	public LoginResponse login(LoginRequest request) {
		String normalizedEmail = emailNormalizer.normalize(request.email());
		User user = userRepository.findByNormalizedEmail(normalizedEmail)
				.orElseThrow(() -> new AuthException(AuthErrorStatus.INVALID_CREDENTIALS));

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new AuthException(AuthErrorStatus.INVALID_CREDENTIALS);
		}
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new UserException(UserErrorStatus.ACCOUNT_NOT_ACTIVE);
		}

		// 사용자와 비밀번호 검증이 끝난 뒤에만 인증 토큰을 발급한다.
		IssuedAccessToken accessToken = accessTokenIssuer.issue(user.getUserId(), Set.of());
		IssuedRefreshSession refreshSession = refreshSessionIssuer.issue(user.getUserId());
		return authResponseConverter.toLoginResponse(
				accessToken,
				refreshSession.tokenValue()
		);
	}
}
