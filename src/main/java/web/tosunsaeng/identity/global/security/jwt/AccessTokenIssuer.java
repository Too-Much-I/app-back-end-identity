package web.tosunsaeng.identity.global.security.jwt;

import java.util.Set;

import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

public interface AccessTokenIssuer {

	/** Account type must come from the current trusted User, never client or token claims. */
	IssuedAccessToken issue(String userId, UserAccountType accountType, Set<String> scopes);
}
