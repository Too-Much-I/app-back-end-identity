package web.tosunsaeng.identity.global.security.jwt;

import java.util.Set;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(String userId, Set<String> scopes);
}
