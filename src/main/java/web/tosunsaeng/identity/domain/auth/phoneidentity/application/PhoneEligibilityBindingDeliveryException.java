package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

public final class PhoneEligibilityBindingDeliveryException extends RuntimeException {

	public enum Kind { CREDENTIAL_UNAVAILABLE, TIMEOUT, CONNECTION }

	private final Kind kind;

	public PhoneEligibilityBindingDeliveryException(Kind kind, Throwable cause) {
		super("Eligibility binding delivery failed: " + kind, cause);
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}
}
