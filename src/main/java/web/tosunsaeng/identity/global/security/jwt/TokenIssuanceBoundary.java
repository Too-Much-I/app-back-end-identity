package web.tosunsaeng.identity.global.security.jwt;

import java.util.function.Supplier;

/** Keeps legacy/test issuers compatible while the real issuer owns the optional transaction boundary. */
public final class TokenIssuanceBoundary {
    private TokenIssuanceBoundary() { }
    public interface Fenced { <T> T withinFence(Supplier<T> action); }
    public static <T> T execute(AccessTokenIssuer issuer, Supplier<T> action) {
        return issuer instanceof Fenced fenced ? fenced.withinFence(action) : action.get();
    }
    public static <T> T withSession(AccessTokenIssuer issuer,
            web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession session, Supplier<T> action) {
        return issuer instanceof JwtAccessTokenIssuer jwt ? jwt.withSession(session, action) : action.get();
    }
}
