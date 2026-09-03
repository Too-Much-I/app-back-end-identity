package web.tosunsaeng.identity.domain.auth.ownerevent.application;

public final class OwnerEventDeliveryException extends RuntimeException {
	public enum Kind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }
	private final Kind kind;

	public OwnerEventDeliveryException(Kind kind, Throwable cause) {
		super(cause);
		this.kind = kind;
	}

	public Kind kind() { return kind; }
}
