package web.tosunsaeng.identity.domain.auth.application.phone;

public enum PhoneIdentityLinkOutcome {

	CREATED,
	IDEMPOTENT,
	ROTATED,
	REPLACED
}
