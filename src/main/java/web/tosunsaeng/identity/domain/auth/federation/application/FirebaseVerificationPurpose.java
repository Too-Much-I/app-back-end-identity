package web.tosunsaeng.identity.domain.auth.federation.application;

public enum FirebaseVerificationPurpose {
	LOGIN_EXCHANGE(false),
	DIRECT_ENROLLMENT(true),
	GUEST_ENROLLMENT(true),
	GUEST_MERGE(false),
	AUTH_METHOD_SYNC(false),
	HIGH_RISK_REAUTHENTICATION(false);

	private final boolean enrollment;

	FirebaseVerificationPurpose(boolean enrollment) {
		this.enrollment = enrollment;
	}

	public boolean requiresEnrollmentEvidence() {
		return enrollment;
	}
}
