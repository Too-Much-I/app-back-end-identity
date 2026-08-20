package web.tosunsaeng.identity.domain.auth.usermerge.application;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
		"eventId", "schemaVersion", "sourceUserId", "targetUserId", "occurredAt"
})
public record UserMergedWireEvent(
		String eventId,
		int schemaVersion,
		String sourceUserId,
		String targetUserId,
		Instant occurredAt
) {

	@Override
	public String toString() {
		return "UserMergedWireEvent[eventId=" + eventId
				+ ", schemaVersion=" + schemaVersion
				+ ", userIds=[REDACTED], occurredAt=" + occurredAt + "]";
	}
}
