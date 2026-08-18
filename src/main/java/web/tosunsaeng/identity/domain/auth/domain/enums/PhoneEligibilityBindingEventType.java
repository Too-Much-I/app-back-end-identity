package web.tosunsaeng.identity.domain.auth.domain.enums;

public enum PhoneEligibilityBindingEventType {

	VERIFIED("PhoneEligibilityBindingVerified"),
	REVOKED("PhoneEligibilityBindingRevoked");

	private final String wireName;

	PhoneEligibilityBindingEventType(String wireName) {
		this.wireName = wireName;
	}

	public String wireName() {
		return wireName;
	}
}
