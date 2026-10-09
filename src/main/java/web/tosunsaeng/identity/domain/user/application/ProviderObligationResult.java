package web.tosunsaeng.identity.domain.user.application;

public enum ProviderObligationResult {
	NOT_REQUIRED,
	/** The approved client contract requires revocation before withdrawal; not server-verified proof. */
	CLIENT_MANAGED,
	SATISFIED
}
