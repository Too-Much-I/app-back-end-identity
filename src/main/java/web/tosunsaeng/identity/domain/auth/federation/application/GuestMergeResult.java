package web.tosunsaeng.identity.domain.auth.federation.application;
import web.tosunsaeng.identity.domain.auth.session.application.IssuedRefreshSession;
public record GuestMergeResult(IssuedRefreshSession refreshSession, String mergeId) {
    @Override public String toString() { return "GuestMergeResult[redacted]"; }
}
