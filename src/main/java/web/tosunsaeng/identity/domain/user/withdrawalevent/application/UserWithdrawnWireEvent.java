package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"eventId", "schemaVersion", "userId", "withdrawnAt"})
public record UserWithdrawnWireEvent(
		String eventId,
		int schemaVersion,
		String userId,
		Instant withdrawnAt
) {

	@Override
	public String toString() {
		return "UserWithdrawnWireEvent[eventId=[REDACTED], schemaVersion="
				+ schemaVersion + ", userId=[REDACTED], withdrawnAt=" + withdrawnAt + "]";
	}
}
