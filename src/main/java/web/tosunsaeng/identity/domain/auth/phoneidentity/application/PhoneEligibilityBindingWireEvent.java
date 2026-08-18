package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
		"eventId", "eventType", "schemaVersion", "producer", "occurredAt",
		"consumerScopeId", "userId", "verifiedAt", "revokedAt", "bindingRevision",
		"fingerprintCandidates"
})
public record PhoneEligibilityBindingWireEvent(
		String eventId,
		String eventType,
		int schemaVersion,
		String producer,
		Instant occurredAt,
		String consumerScopeId,
		String userId,
		Instant verifiedAt,
		Instant revokedAt,
		long bindingRevision,
		List<Candidate> fingerprintCandidates
) {

	@JsonPropertyOrder({"keyVersion", "value"})
	public record Candidate(String keyVersion, String value) {
		@Override
		public String toString() {
			return "Candidate[keyVersion=" + keyVersion + ", value=[REDACTED]]";
		}
	}

	@Override
	public String toString() {
		return "PhoneEligibilityBindingWireEvent[eventId=" + eventId
				+ ", eventType=" + eventType + ", schemaVersion=" + schemaVersion
				+ ", consumerScopeId=" + consumerScopeId + ", bindingRevision="
				+ bindingRevision + ", fingerprintCandidates=[REDACTED]]";
	}
}
