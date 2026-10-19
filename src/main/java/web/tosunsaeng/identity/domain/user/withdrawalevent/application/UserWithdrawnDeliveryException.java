package web.tosunsaeng.identity.domain.user.withdrawalevent.application;

public final class UserWithdrawnDeliveryException extends RuntimeException {

	public enum Kind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }

	private final Kind kind;

	public UserWithdrawnDeliveryException(Kind kind, Throwable cause) {
		super("UserWithdrawn delivery failed.", cause);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}
}
