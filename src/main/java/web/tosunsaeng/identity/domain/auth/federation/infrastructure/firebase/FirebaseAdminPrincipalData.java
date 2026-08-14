package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

record FirebaseAdminPrincipalData(
		String firebaseProjectId,
		String firebaseUid,
		String tenantId,
		String issuer,
		String audience,
		Instant authTime,
		Instant issuedAt,
		Instant expiresAt,
		String signInProviderId,
		boolean emailVerified,
		boolean disabled,
		boolean phoneVerified,
		String verifiedPhoneNumber,
		List<FirebaseLinkedProviderData> linkedProviders
) {

	FirebaseAdminPrincipalData {
		linkedProviders = List.copyOf(Objects.requireNonNull(
				linkedProviders,
				"linkedProviders must not be null"
		));
		if (!phoneVerified && verifiedPhoneNumber != null) {
			throw new IllegalArgumentException("Unverified phone number must be absent");
		}
	}

	@Override
	public String toString() {
		return "FirebaseAdminPrincipalData[identifiers=[REDACTED], signInProviderId="
				+ signInProviderId
				+ ", emailVerified=" + emailVerified
				+ ", disabled=" + disabled
				+ ", phoneVerified=" + phoneVerified
				+ ", verifiedPhoneNumber=[REDACTED]"
				+ ", linkedProviderCount=" + linkedProviders.size() + "]";
	}
}
