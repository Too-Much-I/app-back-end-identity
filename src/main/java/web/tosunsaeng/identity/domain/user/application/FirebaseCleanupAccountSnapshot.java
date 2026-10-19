package web.tosunsaeng.identity.domain.user.application;

import java.util.Objects;
import java.util.Set;

public record FirebaseCleanupAccountSnapshot(
		boolean disabled,
		Set<FirebaseCleanupProvider> providers
) {
	public FirebaseCleanupAccountSnapshot {
		providers = Set.copyOf(Objects.requireNonNull(providers, "providers must not be null"));
	}

	@Override
	public String toString() {
		return "FirebaseCleanupAccountSnapshot[disabled=" + disabled
				+ ", providers=" + providers + "]";
	}
}
