package web.tosunsaeng.identity.security.currentuser;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.common.exception.BusinessException;
import web.tosunsaeng.identity.common.exception.CommonErrorStatus;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

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

		try {
			UUID userId = UUID.fromString(jwt.getSubject());
			if (!userId.toString().equalsIgnoreCase(jwt.getSubject())) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return userId.toString();
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw unauthorized();
		}
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
