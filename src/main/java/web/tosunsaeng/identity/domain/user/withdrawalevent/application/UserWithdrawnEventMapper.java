package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawnOutbox;

public final class UserWithdrawnEventMapper {

	public static final int MAX_PAYLOAD_BYTES = 4 * 1024;

	private final ObjectMapper objectMapper;

	public UserWithdrawnEventMapper(ObjectMapper objectMapper) {
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	public byte[] serialize(UserWithdrawnOutbox event) {
		UserWithdrawnOutbox required = Objects.requireNonNull(event);
		UserWithdrawnWireEvent wireEvent = new UserWithdrawnWireEvent(
				required.getEventId(),
				required.getSchemaVersion(),
				required.getUserId(),
				required.getWithdrawnAt()
		);
		try {
			byte[] payload = objectMapper.writeValueAsBytes(wireEvent);
			if (payload.length > MAX_PAYLOAD_BYTES) {
				throw new IllegalArgumentException("UserWithdrawn payload exceeds 4 KiB.");
			}
			return payload;
		} catch (JsonProcessingException exception) {
			throw new IllegalArgumentException(
					"UserWithdrawn payload could not be serialized.",
					exception
			);
		}
	}
}
