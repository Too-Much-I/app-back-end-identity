const PRIMARY_SIGN_IN_PROVIDERS = new Set([
  "password",
  "google.com",
  "apple.com"
]);

const VERIFICATION_POLICIES = Object.freeze({
  LOGIN_EXCHANGE: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 900,
    requiresRemoteFirebaseCall: true
  }),
  DIRECT_ENROLLMENT: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 300,
    requiresRemoteFirebaseCall: true
  }),
  GUEST_ENROLLMENT: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 300,
    requiresRemoteFirebaseCall: true
  }),
  GUEST_MERGE: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 300,
    requiresRemoteFirebaseCall: true
  }),
  AUTH_METHOD_SYNC: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 300,
    requiresRemoteFirebaseCall: true
  }),
  HIGH_RISK_REAUTHENTICATION: Object.freeze({
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 300,
    requiresRemoteFirebaseCall: true
  })
});

export const NON_FIREBASE_PATH_POLICIES = Object.freeze({
  IDENTITY_REFRESH_SESSION_REISSUE: Object.freeze({
    callsFirebase: false
  }),
  LEARNING_CORE_REQUEST: Object.freeze({
    callsFirebase: false
  })
});

export function verificationPolicy(purpose) {
  const policy = VERIFICATION_POLICIES[purpose];
  if (!policy) {
    throw new Error("UNKNOWN_FIREBASE_VERIFICATION_PURPOSE");
  }
  return policy;
}

export function evaluateIdentityLogin({ signInProvider, kakaoEnabled = false }) {
  if (signInProvider === "phone") {
    return Object.freeze({
      allowed: false,
      code: "PHONE_ONLY_LOGIN_NOT_ALLOWED"
    });
  }

  if (signInProvider === "oidc.kakao") {
    return Object.freeze({
      allowed: kakaoEnabled,
      code: kakaoEnabled ? "ALLOWED" : "PROVIDER_DISABLED"
    });
  }

  if (!PRIMARY_SIGN_IN_PROVIDERS.has(signInProvider)) {
    return Object.freeze({
      allowed: false,
      code: "UNSUPPORTED_SIGN_IN_PROVIDER"
    });
  }

  return Object.freeze({ allowed: true, code: "ALLOWED" });
}

export function evaluateEnrollmentPrincipal({
  linkedProviderIds,
  verifiedPhone,
  emailVerified,
  kakaoEnabled = false
}) {
  if (!Array.isArray(linkedProviderIds)) {
    throw new Error("INVALID_LINKED_PROVIDER_IDS");
  }
  const providers = new Set(linkedProviderIds);
  const hasPassword = providers.has("password");
  const hasEnabledSocialProvider = providers.has("google.com")
    || providers.has("apple.com")
    || (kakaoEnabled && providers.has("oidc.kakao"));

  if (!providers.has("phone") || verifiedPhone !== true) {
    return Object.freeze({ allowed: false, code: "VERIFIED_PHONE_REQUIRED" });
  }
  if (!hasPassword && !hasEnabledSocialProvider) {
    return Object.freeze({ allowed: false, code: "PRIMARY_CREDENTIAL_REQUIRED" });
  }
  if (hasPassword && emailVerified !== true) {
    return Object.freeze({ allowed: false, code: "EMAIL_VERIFICATION_REQUIRED" });
  }
  return Object.freeze({ allowed: true, code: "ALLOWED" });
}

export function enrollmentBinding({
  firebaseProjectId,
  firebaseUid,
  bindingType,
  boundUserId = null
}) {
  requireNonBlank(firebaseProjectId, "firebaseProjectId");
  requireNonBlank(firebaseUid, "firebaseUid");

  if (bindingType === "DIRECT_SIGNUP") {
    if (boundUserId !== null) {
      throw new Error("DIRECT_SIGNUP_MUST_NOT_HAVE_BOUND_USER");
    }
  } else if (bindingType === "GUEST_USER") {
    requireNonBlank(boundUserId, "boundUserId");
  } else {
    throw new Error("UNSUPPORTED_ENROLLMENT_BINDING_TYPE");
  }

  return Object.freeze({
    firebaseProjectId,
    firebaseUid,
    bindingType,
    boundUserId
  });
}

export function resolveGuestMergeProof({
  guestJwtSubject,
  sourceUser,
  firebasePrincipalProjectId,
  firebasePrincipalUid,
  firebaseIdentityOwner
}) {
  requireNonBlank(guestJwtSubject, "guestJwtSubject");
  requireNonBlank(firebasePrincipalProjectId, "firebasePrincipalProjectId");
  requireNonBlank(firebasePrincipalUid, "firebasePrincipalUid");

  if (
    sourceUser?.userId !== guestJwtSubject
    || sourceUser?.accountType !== "GUEST"
    || sourceUser?.status !== "ACTIVE"
  ) {
    throw new Error("INVALID_SOURCE_GUEST_PROOF");
  }

  if (
    firebaseIdentityOwner?.firebaseProjectId !== firebasePrincipalProjectId
    || firebaseIdentityOwner?.firebaseUid !== firebasePrincipalUid
    || firebaseIdentityOwner?.accountType !== "MEMBER"
    || firebaseIdentityOwner?.status !== "ACTIVE"
  ) {
    throw new Error("INVALID_TARGET_FIREBASE_PROOF");
  }
  requireNonBlank(firebaseIdentityOwner.userId, "targetUserId");

  if (sourceUser.userId === firebaseIdentityOwner.userId) {
    throw new Error("MERGE_SOURCE_EQUALS_TARGET");
  }

  return Object.freeze({
    sourceUserId: sourceUser.userId,
    targetUserId: firebaseIdentityOwner.userId
  });
}

function requireNonBlank(value, field) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new Error(`INVALID_${field.toUpperCase()}`);
  }
}
