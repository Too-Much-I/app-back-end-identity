package web.tosunsaeng.identity.domain.auth.usermerge.application;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import web.tosunsaeng.identity.domain.auth.domain.entity.UserMergedOutbox;

public final class UserMergedEventMapper {

	public static final int MAX_PAYLOAD_BYTES = 4 * 1024;

	private final ObjectMapper objectMapper;

	public UserMergedEventMapper(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	public byte[] serialize(UserMergedOutbox event) {
		UserMergedOutbox requiredEvent = Objects.requireNonNull(event);
		UserMergedWireEvent wireEvent = new UserMergedWireEvent(
				requiredEvent.getEventId(),
				requiredEvent.getSchemaVersion(),
				requiredEvent.getSourceUserId(),
				requiredEvent.getTargetUserId(),
				requiredEvent.getOccurredAt()
		);
		try {
			byte[] payload = objectMapper.writeValueAsBytes(wireEvent);
			if (payload.length > MAX_PAYLOAD_BYTES) {
				throw new IllegalArgumentException("UserMerged payload exceeds 4 KiB.");
			}
			return payload;
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException(
					"UserMerged payload could not be serialized.",
					exception
			);
		}
	}
}
