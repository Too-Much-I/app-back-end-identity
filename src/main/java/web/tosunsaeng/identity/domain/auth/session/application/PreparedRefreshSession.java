package web.tosunsaeng.identity.domain.auth.session.application;

import java.util.Map;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;

public record PreparedRefreshSession(
		String tokenValue,
		RefreshSession session,
		Map<String, Long> relatedEpochs
) {

	public PreparedRefreshSession {
		Objects.requireNonNull(tokenValue, "tokenValue must not be null");
		Objects.requireNonNull(session, "session must not be null");
		relatedEpochs = Map.copyOf(relatedEpochs);
	}

	public PreparedRefreshSession(String tokenValue, RefreshSession session) {
		this(tokenValue, session, Map.of());
	}

	@Override
	public String toString() {
		return "PreparedRefreshSession[tokenValue=redacted, session=redacted]";
	}
}
