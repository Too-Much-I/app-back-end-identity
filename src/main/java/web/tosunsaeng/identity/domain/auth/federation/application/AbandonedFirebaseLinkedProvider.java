package web.tosunsaeng.identity.domain.auth.federation.application;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

public record AbandonedFirebaseLinkedProvider(
		SocialProvider provider,
		String providerSubject
) {
	public AbandonedFirebaseLinkedProvider {
		Objects.requireNonNull(provider, "provider must not be null");
		String required = Objects.requireNonNull(
				providerSubject, "providerSubject must not be null"
		);
		if (required.isBlank() || required.length() > 255) {
			throw new IllegalArgumentException("providerSubject must be a valid opaque value");
		}
	}

	@Override
	public String toString() {
		return "AbandonedFirebaseLinkedProvider[provider=" + provider
				+ ", providerSubject=[REDACTED]]";
	}
}
