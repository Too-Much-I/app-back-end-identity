package web.tosunsaeng.identity.global.security.currentuser;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.user.domain.enums.UserStatus;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

	private final UserRepository userRepository;

	public JwtCurrentUserProvider(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public String getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			throw unauthorized();
		}

		Jwt jwt = extractJwt(authentication);
		if (jwt == null) {
			throw unauthorized();
		}

		String canonicalUserId;
		try {
			// 클라이언트 입력이 아닌 검증된 JWT subject에서 사용자 ID를 가져온다.
			UUID userId = UUID.fromString(jwt.getSubject());
			if (!userId.toString().equalsIgnoreCase(jwt.getSubject())) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			canonicalUserId = userId.toString();
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw unauthorized();
		}
		if (userRepository.existsByUserIdAndStatus(canonicalUserId, UserStatus.MERGED)) {
			// MERGED source JWT를 target 권한으로 치환하지 않고 즉시 거부한다.
			throw new AuthException(AuthErrorStatus.ACCOUNT_MERGED_TOKEN_REJECTED);
		}
		return canonicalUserId;
	}

	private Jwt extractJwt(Authentication authentication) {
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			return jwtAuthentication.getToken();
		}
		return authentication.getPrincipal() instanceof Jwt jwt ? jwt : null;
	}

	private BusinessException unauthorized() {
		return new BusinessException(CommonErrorStatus.UNAUTHORIZED);
	}
}
