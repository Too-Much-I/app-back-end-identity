package web.tosunsaeng.identity.domain.auth.application.phone;

import java.util.Objects;
import java.util.UUID;

public record PhoneIdentityLinkResult(
		String phoneIdentityId,
		String userId,
		PhoneIdentityLinkOutcome outcome
) {

	public PhoneIdentityLinkResult {
		phoneIdentityId = requireUuid(phoneIdentityId, "phoneIdentityId");
		userId = requireUuid(userId, "userId");
		Objects.requireNonNull(outcome, "outcome must not be null");
	}

	private static String requireUuid(String value, String fieldName) {
		try {
			UUID parsed = UUID.fromString(value);
			if (!parsed.toString().equalsIgnoreCase(value)) {
				throw new IllegalArgumentException("non-canonical UUID");
			}
			return value;
		} catch (IllegalArgumentException | NullPointerException exception) {
			throw new IllegalArgumentException(fieldName + " must be a UUID.");
		}
	}
}
