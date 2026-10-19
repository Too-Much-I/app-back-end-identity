package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;

public final class OwnerEventWireMapper {
	private final ObjectMapper objectMapper;

	public OwnerEventWireMapper(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	public byte[] serialize(OwnerEventCore event) {
		Objects.requireNonNull(event);
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("eventId", event.getEventId());
		if (event.getEventType() == OwnerEventType.TRIAL_OWNER_REBIND_APPROVED) {
			payload.put("eventType", "TrialOwnerRebindApproved");
		}
		payload.put("schemaVersion", event.getSchemaVersion());
		if (event.getEventType() == OwnerEventType.TRIAL_OWNER_REBIND_APPROVED) {
			payload.put("producer", event.getProducer());
			payload.put("consumerScopeId", event.getConsumerScopeId());
		}
		payload.put("occurredAt", event.getOccurredAt());
		payload.put("sourceUserId", event.getSourceUserId());
		payload.put("targetUserId", event.getTargetUserId());
		if (event.getEventType() == OwnerEventType.TRIAL_OWNER_REBIND_APPROVED) {
			payload.put("lifecycleReason", event.getLifecycleReason());
			payload.put("sourceBindingRevision", event.getSourceBindingRevision());
			payload.put("targetBindingRevision", event.getTargetBindingRevision());
		}
		try {
			return objectMapper.writeValueAsBytes(payload);
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("Owner event serialization failed", exception);
		}
	}
}
