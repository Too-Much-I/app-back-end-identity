package web.tosunsaeng.identity.domain.auth.infrastructure.firebase;

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
		List<FirebaseLinkedProviderData> linkedProviders
) {

	FirebaseAdminPrincipalData {
		linkedProviders = List.copyOf(Objects.requireNonNull(
				linkedProviders,
				"linkedProviders must not be null"
		));
	}

	@Override
	public String toString() {
		return "FirebaseAdminPrincipalData[identifiers=[REDACTED], signInProviderId="
				+ signInProviderId
				+ ", emailVerified=" + emailVerified
				+ ", disabled=" + disabled
				+ ", phoneVerified=" + phoneVerified
				+ ", linkedProviderCount=" + linkedProviders.size() + "]";
	}
}
