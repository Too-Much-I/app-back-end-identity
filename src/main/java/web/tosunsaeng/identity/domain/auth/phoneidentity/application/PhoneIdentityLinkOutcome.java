package web.tosunsaeng.identity.domain.auth.phoneidentity.application;

public enum PhoneIdentityLinkOutcome {

	CREATED,
	IDEMPOTENT,
	ROTATED,
	REPLACED
}
