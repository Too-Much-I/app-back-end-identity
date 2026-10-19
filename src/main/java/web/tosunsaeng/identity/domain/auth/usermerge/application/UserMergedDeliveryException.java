package web.tosunsaeng.identity.domain.auth.usermerge.application;

import java.util.Objects;

public final class UserMergedDeliveryException extends RuntimeException {

	public enum Kind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }

	private final Kind kind;

	public UserMergedDeliveryException(Kind kind, Throwable cause) {
		super("UserMerged delivery failed.", cause);
		this.kind = Objects.requireNonNull(kind);
	}

	public Kind kind() {
		return kind;
	}
}
