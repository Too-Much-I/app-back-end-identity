package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.List;
import java.util.Objects;

public record AbandonedFirebaseAccountSnapshot(
		boolean disabled,
		String verifiedPhoneNumber,
		List<AbandonedFirebaseLinkedProvider> linkedProviders
) {
	public AbandonedFirebaseAccountSnapshot {
		linkedProviders = List.copyOf(Objects.requireNonNull(linkedProviders));
	}

	@Override
	public String toString() {
		return "AbandonedFirebaseAccountSnapshot[disabled=" + disabled
				+ ", hasVerifiedPhone=" + (verifiedPhoneNumber != null)
				+ ", linkedProviderCount=" + linkedProviders.size()
				+ ", identifiers=[REDACTED]]";
	}
}
