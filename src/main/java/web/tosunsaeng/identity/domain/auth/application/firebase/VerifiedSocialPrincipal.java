package web.tosunsaeng.identity.domain.auth.application.firebase;

import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

public record VerifiedSocialPrincipal(
		SocialProvider provider,
		String providerSubject
) {

	private static final int MAX_PROVIDER_SUBJECT_LENGTH = 255;

	public VerifiedSocialPrincipal {
		provider = Objects.requireNonNull(provider, "provider must not be null");
		providerSubject = requireProviderSubject(providerSubject);
	}

	private static String requireProviderSubject(String value) {
		String required = Objects.requireNonNull(value, "providerSubject must not be null");
		if (required.isBlank() || required.length() > MAX_PROVIDER_SUBJECT_LENGTH) {
			throw new IllegalArgumentException(
					"providerSubject must be between 1 and 255 characters"
			);
		}
		return required;
	}

	@Override
	public String toString() {
		return "VerifiedSocialPrincipal[provider=" + provider + ", providerSubject=[REDACTED]]";
	}
}
