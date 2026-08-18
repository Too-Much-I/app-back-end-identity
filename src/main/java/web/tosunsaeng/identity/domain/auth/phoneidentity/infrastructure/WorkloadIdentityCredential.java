package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record WorkloadIdentityCredential(String tokenValue, Instant issuedAt, Instant expiresAt) {

	public WorkloadIdentityCredential {
		if (tokenValue == null || tokenValue.isBlank()
				|| tokenValue.chars().anyMatch(Character::isWhitespace)) {
			throw new IllegalArgumentException("credential token must be a non-blank token");
		}
		Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (!expiresAt.isAfter(issuedAt)
				|| Duration.between(issuedAt, expiresAt).compareTo(Duration.ofMinutes(5)) > 0) {
			throw new IllegalArgumentException("credential lifetime must be positive and at most five minutes");
		}
	}

	@Override
	public String toString() {
		return "WorkloadIdentityCredential[tokenValue=[REDACTED], issuedAt=" + issuedAt
				+ ", expiresAt=" + expiresAt + "]";
	}
}
