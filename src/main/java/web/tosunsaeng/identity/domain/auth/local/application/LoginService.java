package web.tosunsaeng.identity.domain.auth.local.application;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import web.tosunsaeng.identity.domain.auth.common.converter.AuthResponseConverter;
import web.tosunsaeng.identity.domain.auth.local.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.local.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.RefreshSessionIssuer;
import web.tosunsaeng.identity.domain.user.domain.EmailNormalizer;
import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;
import web.tosunsaeng.identity.global.security.jwt.AccessTokenIssuer;
import web.tosunsaeng.identity.global.security.jwt.IssuedAccessToken;

@Service
@RequiredArgsConstructor
public class LoginService {

	private static final Logger log = LoggerFactory.getLogger(LoginService.class);

	private final UserRepository userRepository;
	private final EmailNormalizer emailNormalizer;
	private final PasswordEncoder passwordEncoder;
	private final AccessTokenIssuer accessTokenIssuer;
	private final RefreshSessionIssuer refreshSessionIssuer;
	private final AuthResponseConverter authResponseConverter;

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
		LoginResponse response = authResponseConverter.toLoginResponse(
				accessToken,
				refreshSession.tokenValue()
		);
		log.atInfo()
				.addKeyValue("event", "auth.login.succeeded")
				.addKeyValue("outcome", "authenticated")
				.addKeyValue("userId", user.getUserId())
				.addKeyValue("accountType", user.getAccountType())
				.addKeyValue("provider", user.getProvider())
				.log("로그인에 성공했습니다");
		return response;
	}
}
