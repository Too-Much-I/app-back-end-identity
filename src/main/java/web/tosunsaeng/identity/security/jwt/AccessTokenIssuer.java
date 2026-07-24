package web.tosunsaeng.identity.security.jwt;

import java.util.Set;

public interface AccessTokenIssuer {

	IssuedAccessToken issue(String userId, Set<String> scopes);
}
