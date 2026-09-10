package web.tosunsaeng.identity.domain.auth.session.application;

import java.time.Instant;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;

/** Internal result; the HTTP JSON remains ReissueResponse. */
public record ReissueResult(ReissueResponse response, Instant accessExpiresAt, Instant refreshExpiresAt,
		Instant recoveryUntil) {
	@Override public String toString() { return "ReissueResult[redacted]"; }
}
