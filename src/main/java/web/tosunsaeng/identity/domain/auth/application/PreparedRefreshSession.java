package web.tosunsaeng.identity.domain.auth.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;

record PreparedRefreshSession(
		String tokenValue,
		RefreshSession session
) {

	PreparedRefreshSession {
		Objects.requireNonNull(tokenValue, "tokenValue must not be null");
		Objects.requireNonNull(session, "session must not be null");
	}

	@Override
	public String toString() {
		return "PreparedRefreshSession[tokenValue=redacted, session=redacted]";
	}
}
