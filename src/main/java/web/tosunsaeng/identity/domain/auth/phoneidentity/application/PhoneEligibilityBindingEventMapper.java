package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingEventType;

public final class PhoneEligibilityBindingEventMapper {

	public static final int MAX_PAYLOAD_BYTES = 16 * 1024;

	private final ObjectMapper objectMapper;

	public PhoneEligibilityBindingEventMapper(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	public byte[] serialize(PhoneEligibilityBindingOutbox event) {
		Objects.requireNonNull(event);
		List<PhoneEligibilityBindingWireEvent.Candidate> candidates = event.getEventType()
				== PhoneEligibilityBindingEventType.VERIFIED
				? event.getFingerprintCandidates().stream()
						.map(candidate -> new PhoneEligibilityBindingWireEvent.Candidate(
								candidate.keyVersion(), candidate.fingerprint()))
						.toList()
				: null;
		PhoneEligibilityBindingWireEvent wireEvent = new PhoneEligibilityBindingWireEvent(
				event.getEventId(), event.getEventType().wireName(), event.getSchemaVersion(),
				event.getProducer(), event.getOccurredAt(), event.getConsumerScopeId(),
				event.getUserId(), event.getVerifiedAt(), event.getRevokedAt(),
				event.getBindingRevision(), candidates
		);
		try {
			byte[] payload = objectMapper.writeValueAsBytes(wireEvent);
			if (payload.length > MAX_PAYLOAD_BYTES) {
				throw new IllegalArgumentException("Eligibility binding payload exceeds 16 KiB.");
			}
			return payload;
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Eligibility binding payload could not be serialized.", exception);
		}
	}
}
