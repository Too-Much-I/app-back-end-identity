package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.util.Objects;

record FirebaseLinkedProviderData(
		String providerId,
		String providerUid
) {

	FirebaseLinkedProviderData {
		providerId = Objects.requireNonNull(providerId, "providerId must not be null");
	}

	@Override
	public String toString() {
		return "FirebaseLinkedProviderData[providerId=" + providerId
				+ ", providerUid=[REDACTED]]";
	}
}
