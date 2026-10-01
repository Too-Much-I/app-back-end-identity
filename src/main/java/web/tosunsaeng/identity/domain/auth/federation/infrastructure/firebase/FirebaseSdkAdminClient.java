package web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserRecord;

final class FirebaseSdkAdminClient implements FirebaseAdminClient {

	private static final String PASSWORD_PROVIDER_ID = "password";
	private static final String PHONE_PROVIDER_ID = "phone";

	private final AbstractFirebaseAuth firebaseAuth;
	private final String firebaseProjectId;
	private final String kakaoProviderId;

	FirebaseSdkAdminClient(FirebaseApp firebaseApp, FirebaseAuthProperties properties) {
		this(
				selectFirebaseAuth(firebaseApp, properties.tenantId()),
				properties.projectId(), properties.kakaoProviderId()
		);
	}

	FirebaseSdkAdminClient(AbstractFirebaseAuth firebaseAuth, String firebaseProjectId) {
		this(firebaseAuth, firebaseProjectId, "oidc.kakao");
	}

	FirebaseSdkAdminClient(AbstractFirebaseAuth firebaseAuth, String firebaseProjectId, String kakaoProviderId) {
		this.kakaoProviderId = Objects.requireNonNull(kakaoProviderId, "kakaoProviderId must not be null");
		this.firebaseAuth = Objects.requireNonNull(firebaseAuth, "firebaseAuth must not be null");
		this.firebaseProjectId = Objects.requireNonNull(
				firebaseProjectId,
				"firebaseProjectId must not be null"
		);
	}

	@Override
	public FirebaseAdminPrincipalData verify(String firebaseIdToken, boolean checkRevoked) {
		try {
			FirebaseToken token = firebaseAuth.verifyIdToken(firebaseIdToken, checkRevoked);
			String firebaseUid = token.getUid();
			if (firebaseUid == null || firebaseUid.isBlank()) {
				throw invalidToken();
			}
			UserRecord user = firebaseAuth.getUser(firebaseUid);
			return toPrincipalData(token, user);
		} catch (FirebaseAuthException exception) {
			throw classify(exception);
		} catch (FirebaseAdminClientException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new FirebaseAdminClientException(
					FirebaseAdminClientException.Reason.UNAVAILABLE
			);
		}
	}

	private FirebaseAdminPrincipalData toPrincipalData(
			FirebaseToken token,
			UserRecord user
	) {
		Map<String, Object> claims = token.getClaims();
		if (claims == null) {
			throw invalidToken();
		}
		List<FirebaseLinkedProviderData> linkedProviders = linkedProviders(user);
		validateCurrentSocialSubject(claims, linkedProviders);
		boolean hasPhoneProvider = linkedProviders.stream()
				.anyMatch(provider -> PHONE_PROVIDER_ID.equals(provider.providerId()));
		boolean phoneVerified = hasPhoneProvider
				&& user.getPhoneNumber() != null
				&& !user.getPhoneNumber().isBlank();
		String verifiedPhoneNumber = phoneVerified ? user.getPhoneNumber() : null;

		return new FirebaseAdminPrincipalData(
				firebaseProjectId,
				token.getUid(),
				token.getTenantId(),
				token.getIssuer(),
				requireStringClaim(claims, "aud"),
				requireInstantClaim(claims, "auth_time"),
				requireInstantClaim(claims, "iat"),
				requireInstantClaim(claims, "exp"),
				requireSignInProvider(claims),
				user.isEmailVerified(),
				user.isDisabled(),
				phoneVerified,
				verifiedPhoneNumber,
				linkedProviders
		);
	}

	private static AbstractFirebaseAuth selectFirebaseAuth(
			FirebaseApp firebaseApp,
			String tenantId
	) {
		FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(
				Objects.requireNonNull(firebaseApp, "firebaseApp must not be null")
		);
		return tenantId == null
				? firebaseAuth
				: firebaseAuth.getTenantManager().getAuthForTenant(tenantId);
	}

	private static List<FirebaseLinkedProviderData> linkedProviders(UserRecord user) {
		List<FirebaseLinkedProviderData> linkedProviders = new ArrayList<>();
		UserInfo[] providerData = user.getProviderData();
		if (providerData == null) {
			return linkedProviders;
		}
		for (UserInfo provider : providerData) {
			String providerId = provider.getProviderId();
			if (providerId == null || providerId.isBlank()) {
				continue;
			}
			String providerUid = PASSWORD_PROVIDER_ID.equals(providerId)
					|| PHONE_PROVIDER_ID.equals(providerId)
					? null
					: provider.getUid();
			linkedProviders.add(new FirebaseLinkedProviderData(providerId, providerUid));
		}
		return linkedProviders;
	}

	private static String requireSignInProvider(Map<String, Object> claims) {
		Object firebaseClaim = claims.get("firebase");
		if (!(firebaseClaim instanceof Map<?, ?> firebaseValues)) {
			throw invalidToken();
		}
		Object provider = firebaseValues.get("sign_in_provider");
		if (!(provider instanceof String providerId) || providerId.isBlank()) {
			throw invalidToken();
		}
		return providerId;
	}

	/** A latest Admin snapshot alone cannot prove which external account signed this token. */
	private void validateCurrentSocialSubject(Map<String, Object> claims,
			List<FirebaseLinkedProviderData> linkedProviders) {
		String provider = requireSignInProvider(claims);
		if (!provider.equals("google.com") && !provider.equals("apple.com") && !provider.equals(kakaoProviderId)) return;
		var firebase = (Map<?, ?>) claims.get("firebase");
		if (!(firebase.get("identities") instanceof Map<?, ?> identities)
				|| !(identities.get(provider) instanceof List<?> subjects)
				|| subjects.size() != 1 || !(subjects.getFirst() instanceof String subject) || subject.isBlank()) {
			throw invalidToken();
		}
		var current = linkedProviders.stream().filter(p -> provider.equals(p.providerId())).toList();
		if (current.size() != 1 || !subject.equals(current.getFirst().providerUid())) throw invalidToken();
	}

	private static String requireStringClaim(Map<String, Object> claims, String claimName) {
		Object value = claims.get(claimName);
		if (!(value instanceof String text) || text.isBlank()) {
			throw invalidToken();
		}
		return text;
	}

	private static Instant requireInstantClaim(Map<String, Object> claims, String claimName) {
		Object value = claims.get(claimName);
		if (!(value instanceof Number number)) {
			throw invalidToken();
		}
		try {
			return Instant.ofEpochSecond(number.longValue());
		} catch (DateTimeException exception) {
			throw invalidToken();
		}
	}

	private static FirebaseAdminClientException classify(FirebaseAuthException exception) {
		AuthErrorCode authErrorCode = exception.getAuthErrorCode();
		if (authErrorCode == AuthErrorCode.USER_DISABLED
				|| authErrorCode == AuthErrorCode.USER_NOT_FOUND) {
			return new FirebaseAdminClientException(
					FirebaseAdminClientException.Reason.ACCOUNT_NOT_ALLOWED
			);
		}
		if (authErrorCode == AuthErrorCode.CERTIFICATE_FETCH_FAILED
				|| authErrorCode == AuthErrorCode.CONFIGURATION_NOT_FOUND
				|| authErrorCode == AuthErrorCode.TENANT_NOT_FOUND) {
			return new FirebaseAdminClientException(
					FirebaseAdminClientException.Reason.UNAVAILABLE
			);
		}

		ErrorCode errorCode = exception.getErrorCode();
		if (errorCode == ErrorCode.RESOURCE_EXHAUSTED) {
			return new FirebaseAdminClientException(
					FirebaseAdminClientException.Reason.RATE_LIMITED
			);
		}
		if (errorCode == ErrorCode.PERMISSION_DENIED
				|| errorCode == ErrorCode.UNAUTHENTICATED
				|| errorCode == ErrorCode.UNAVAILABLE
				|| errorCode == ErrorCode.DEADLINE_EXCEEDED
				|| errorCode == ErrorCode.INTERNAL
				|| errorCode == ErrorCode.UNKNOWN) {
			return new FirebaseAdminClientException(
					FirebaseAdminClientException.Reason.UNAVAILABLE
			);
		}
		return invalidToken();
	}

	private static FirebaseAdminClientException invalidToken() {
		return new FirebaseAdminClientException(
				FirebaseAdminClientException.Reason.INVALID_TOKEN
		);
	}
}
