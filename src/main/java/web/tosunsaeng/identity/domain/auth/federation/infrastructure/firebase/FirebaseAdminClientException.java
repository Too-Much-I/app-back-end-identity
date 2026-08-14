package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

final class FirebaseAdminClientException extends RuntimeException {

	enum Reason {
		INVALID_TOKEN,
		ACCOUNT_NOT_ALLOWED,
		RATE_LIMITED,
		UNAVAILABLE
	}

	private final Reason reason;

	FirebaseAdminClientException(Reason reason) {
		super("Firebase Admin verification failed.");
		this.reason = reason;
	}

	Reason reason() {
		return reason;
	}
}
