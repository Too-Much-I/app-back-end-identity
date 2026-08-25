package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseAuthenticationVerifier;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseVerificationPurpose;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedFirebasePrincipal;
import web.tosunsaeng.identity.domain.auth.federation.application.VerifiedSocialPrincipal;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

public final class FirebaseAdminAuthenticationVerifier
		implements FirebaseAuthenticationVerifier {

	private static final int MAX_FIREBASE_ID_TOKEN_LENGTH = 16_384;
	private static final int MAX_FIREBASE_UID_LENGTH = 128;
	private static final Set<FirebaseAuthenticationMethod> PRIMARY_METHODS = Set.of(
			FirebaseAuthenticationMethod.PASSWORD,
			FirebaseAuthenticationMethod.GOOGLE,
			FirebaseAuthenticationMethod.APPLE,
			FirebaseAuthenticationMethod.KAKAO
	);

	private final FirebaseAdminClient firebaseAdminClient;
	private final FirebaseAuthProperties properties;
	private final Clock clock;

	FirebaseAdminAuthenticationVerifier(
			FirebaseAdminClient firebaseAdminClient,
			FirebaseAuthProperties properties,
			Clock clock
	) {
		this.firebaseAdminClient = Objects.requireNonNull(
				firebaseAdminClient,
				"firebaseAdminClient must not be null"
		);
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public VerifiedFirebasePrincipal verify(
			String firebaseIdToken,
			FirebaseVerificationPurpose purpose
	) {
		String requiredToken = requireToken(firebaseIdToken);
		FirebaseVerificationPurpose requiredPurpose = Objects.requireNonNull(
				purpose,
				"purpose must not be null"
		);

		FirebaseAdminPrincipalData data;
		try {
			// Firebase를 사용하는 모든 목적은 revoke와 disabled 상태를 원격 확인한다.
			data = firebaseAdminClient.verify(requiredToken, true);
		} catch (FirebaseAdminClientException exception) {
			throw mapClientException(exception);
		}

		validateIdentityAndTime(data, requiredPurpose);
		ProviderResolution providers = resolveProviders(data, requiredPurpose);
		validateProviderPolicy(data, providers, requiredPurpose);

		try {
			return new VerifiedFirebasePrincipal(
					data.firebaseProjectId(),
					data.firebaseUid(),
					providers.signInMethod(),
					data.authTime(),
					data.issuedAt(),
					data.expiresAt(),
					data.emailVerified(),
					data.phoneVerified(),
					data.verifiedPhoneNumber(),
					providers.linkedMethods(),
					providers.socialPrincipals()
			);
		} catch (IllegalArgumentException exception) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
		}
	}

	private void validateIdentityAndTime(
			FirebaseAdminPrincipalData data,
			FirebaseVerificationPurpose purpose
	) {
		if (data == null
				|| !Objects.equals(properties.projectId(), data.firebaseProjectId())
				|| !Objects.equals(properties.projectId(), data.audience())
				|| !Objects.equals(properties.expectedIssuer(), data.issuer())
				|| !Objects.equals(properties.tenantId(), normalizeOptional(data.tenantId()))
				|| data.firebaseUid() == null
				|| data.firebaseUid().isBlank()
				|| data.firebaseUid().length() > MAX_FIREBASE_UID_LENGTH
				|| data.authTime() == null
				|| data.issuedAt() == null
				|| data.expiresAt() == null) {
			throw new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN);
		}
		if (data.disabled()) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
		}

		Instant now = clock.instant();
		Instant latestAllowedTime = now.plus(properties.clockSkew());
		if (data.authTime().isAfter(latestAllowedTime)
				|| data.issuedAt().isAfter(latestAllowedTime)
				|| !data.expiresAt().isAfter(now.minus(properties.clockSkew()))
				|| data.authTime().isAfter(data.issuedAt())
				|| !data.expiresAt().isAfter(data.issuedAt())) {
			throw new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN);
		}

		Duration maxAuthenticationAge = purpose == FirebaseVerificationPurpose.LOGIN_EXCHANGE
				? properties.loginMaxAuthenticationAge()
				: properties.highRiskMaxAuthenticationAge();
		if (data.authTime().isBefore(now.minus(maxAuthenticationAge))) {
			throw new AuthException(AuthErrorStatus.FIREBASE_RECENT_AUTH_REQUIRED);
		}
	}

	private ProviderResolution resolveProviders(
			FirebaseAdminPrincipalData data,
			FirebaseVerificationPurpose purpose
	) {
		FirebaseAuthenticationMethod signInMethod = methodFor(data.signInProviderId());
		if (signInMethod == null || !isAllowedForPurpose(signInMethod, purpose)) {
			throw new AuthException(AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED);
		}

		EnumSet<FirebaseAuthenticationMethod> linkedMethods = EnumSet.noneOf(
				FirebaseAuthenticationMethod.class
		);
		Map<SocialProvider, VerifiedSocialPrincipal> socialPrincipals = new LinkedHashMap<>();
		for (FirebaseLinkedProviderData provider : data.linkedProviders()) {
			FirebaseAuthenticationMethod method = methodFor(provider.providerId());
			if (method == null || !isAllowedForPurpose(method, purpose)) {
				continue;
			}
			linkedMethods.add(method);
			SocialProvider socialProvider = socialProviderFor(method);
			if (socialProvider != null) {
				String providerUid = provider.providerUid();
				if (providerUid == null || providerUid.isBlank()) {
					throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
				}
				try {
					socialPrincipals.put(
							socialProvider,
							new VerifiedSocialPrincipal(socialProvider, providerUid)
					);
				} catch (IllegalArgumentException exception) {
					throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
				}
			}
		}

		List<VerifiedSocialPrincipal> sortedSocialPrincipals = new ArrayList<>(
				socialPrincipals.values()
		);
		sortedSocialPrincipals.sort(Comparator.comparing(VerifiedSocialPrincipal::provider));
		return new ProviderResolution(
				signInMethod,
				Set.copyOf(linkedMethods),
				List.copyOf(sortedSocialPrincipals)
		);
	}

	private void validateProviderPolicy(
			FirebaseAdminPrincipalData data,
			ProviderResolution providers,
			FirebaseVerificationPurpose purpose
	) {
		if (!isAllowedForPurpose(providers.signInMethod(), purpose)) {
			throw new AuthException(AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED);
		}
		if (!providers.linkedMethods().contains(providers.signInMethod())) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
		}
		boolean hasPrimaryMethod = providers.linkedMethods().stream()
				.anyMatch(PRIMARY_METHODS::contains);
		if (!hasPrimaryMethod) {
			throw new AuthException(AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED);
		}
		if ((purpose == FirebaseVerificationPurpose.LOGIN_EXCHANGE
				|| purpose == FirebaseVerificationPurpose.WITHDRAWAL)
				&& providers.signInMethod() == FirebaseAuthenticationMethod.PHONE) {
			throw new AuthException(AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED);
		}
		if (!purpose.requiresEnrollmentEvidence()) {
			return;
		}
		if (!providers.linkedMethods().contains(FirebaseAuthenticationMethod.PHONE)
				|| !data.phoneVerified()
				|| data.verifiedPhoneNumber() == null
				|| data.verifiedPhoneNumber().isBlank()) {
			throw new AuthException(AuthErrorStatus.FIREBASE_PHONE_VERIFICATION_REQUIRED);
		}
		if (providers.linkedMethods().contains(FirebaseAuthenticationMethod.PASSWORD)
				&& !data.emailVerified()) {
			throw new AuthException(AuthErrorStatus.FIREBASE_EMAIL_VERIFICATION_REQUIRED);
		}
	}

	private boolean isAllowedForPurpose(
			FirebaseAuthenticationMethod method,
			FirebaseVerificationPurpose purpose
	) {
		return purpose == FirebaseVerificationPurpose.WITHDRAWAL || isEnabled(method);
	}

	private FirebaseAuthenticationMethod methodFor(String providerId) {
		if (providerId == null) {
			return null;
		}
		return switch (providerId) {
			case "password" -> FirebaseAuthenticationMethod.PASSWORD;
			case "google.com" -> FirebaseAuthenticationMethod.GOOGLE;
			case "apple.com" -> FirebaseAuthenticationMethod.APPLE;
			case "phone" -> FirebaseAuthenticationMethod.PHONE;
			default -> properties.kakaoProviderId().equals(providerId)
					? FirebaseAuthenticationMethod.KAKAO
					: null;
		};
	}

	private boolean isEnabled(FirebaseAuthenticationMethod method) {
		return switch (method) {
			case PASSWORD -> true;
			case GOOGLE -> properties.googleEnabled();
			case APPLE -> properties.appleEnabled();
			case KAKAO -> properties.kakaoEnabled();
			case PHONE -> properties.phoneEnabled();
		};
	}

	private static SocialProvider socialProviderFor(FirebaseAuthenticationMethod method) {
		return switch (method) {
			case GOOGLE -> SocialProvider.GOOGLE;
			case APPLE -> SocialProvider.APPLE;
			case KAKAO -> SocialProvider.KAKAO;
			case PASSWORD, PHONE -> null;
		};
	}

	private static AuthException mapClientException(FirebaseAdminClientException exception) {
		return switch (exception.reason()) {
			case INVALID_TOKEN -> new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN);
			case ACCOUNT_NOT_ALLOWED -> new AuthException(
					AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED
			);
			case RATE_LIMITED -> new AuthException(AuthErrorStatus.FIREBASE_RATE_LIMITED);
			case UNAVAILABLE -> new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
		};
	}

	private static String requireToken(String firebaseIdToken) {
		if (firebaseIdToken == null
				|| firebaseIdToken.isBlank()
				|| firebaseIdToken.length() > MAX_FIREBASE_ID_TOKEN_LENGTH) {
			throw new AuthException(AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN);
		}
		return firebaseIdToken;
	}

	private static String normalizeOptional(String value) {
		return value == null || value.isBlank() ? null : value;
	}

	private record ProviderResolution(
			FirebaseAuthenticationMethod signInMethod,
			Set<FirebaseAuthenticationMethod> linkedMethods,
			List<VerifiedSocialPrincipal> socialPrincipals
	) {
	}
}
