package web.tosunsaeng.identity.domain.auth.federation.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record VerifiedFirebasePrincipal(
		String firebaseProjectId,
		String firebaseUid,
		FirebaseAuthenticationMethod signInMethod,
		Instant authTime,
		Instant issuedAt,
		Instant expiresAt,
		boolean emailVerified,
		boolean phoneVerified,
		String verifiedPhoneNumber,
		Set<FirebaseAuthenticationMethod> linkedMethods,
		List<VerifiedSocialPrincipal> linkedSocialPrincipals
) {

	private static final int MAX_FIREBASE_UID_LENGTH = 128;

	public VerifiedFirebasePrincipal {
		firebaseProjectId = requireNonBlank(firebaseProjectId, "firebaseProjectId");
		firebaseUid = requireUid(firebaseUid);
		signInMethod = Objects.requireNonNull(signInMethod, "signInMethod must not be null");
		authTime = Objects.requireNonNull(authTime, "authTime must not be null");
		issuedAt = Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		linkedMethods = Set.copyOf(Objects.requireNonNull(
				linkedMethods,
				"linkedMethods must not be null"
		));
		linkedSocialPrincipals = List.copyOf(Objects.requireNonNull(
				linkedSocialPrincipals,
				"linkedSocialPrincipals must not be null"
		));
		verifiedPhoneNumber = normalizeVerifiedPhone(phoneVerified, verifiedPhoneNumber);
	}

	private static String normalizeVerifiedPhone(boolean phoneVerified, String value) {
		if (!phoneVerified) {
			if (value != null) {
				throw new IllegalArgumentException("Unverified phone number must be absent");
			}
			return null;
		}
		String required = Objects.requireNonNull(value, "verifiedPhoneNumber must not be null");
		if (required.isBlank()) {
			throw new IllegalArgumentException("verifiedPhoneNumber must not be blank");
		}
		return required;
	}

	private static String requireUid(String value) {
		String required = requireNonBlank(value, "firebaseUid");
		if (required.length() > MAX_FIREBASE_UID_LENGTH) {
			throw new IllegalArgumentException("firebaseUid must be at most 128 characters");
		}
		return required;
	}

	private static String requireNonBlank(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null");
		if (required.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return required;
	}

	@Override
	public String toString() {
		return "VerifiedFirebasePrincipal[firebaseProjectId=[REDACTED], "
				+ "firebaseUid=[REDACTED], signInMethod=" + signInMethod
				+ ", authTime=" + authTime
				+ ", issuedAt=" + issuedAt
				+ ", expiresAt=" + expiresAt
				+ ", emailVerified=" + emailVerified
				+ ", phoneVerified=" + phoneVerified
				+ ", verifiedPhoneNumber=[REDACTED]"
				+ ", linkedMethods=" + linkedMethods
				+ ", linkedSocialPrincipalCount=" + linkedSocialPrincipals.size() + "]";
	}
}
