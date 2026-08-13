import assert from "node:assert/strict";
import test from "node:test";

import {
  NON_FIREBASE_PATH_POLICIES,
  enrollmentBinding,
  evaluateEnrollmentPrincipal,
  evaluateIdentityLogin,
  resolveGuestMergeProof,
  verificationPolicy
} from "../src/identity-policy.mjs";

test("email, Google, Apple은 허용하고 phone-only 로그인은 거절한다", () => {
  for (const signInProvider of ["password", "google.com", "apple.com"]) {
    assert.deepEqual(evaluateIdentityLogin({ signInProvider }), {
      allowed: true,
      code: "ALLOWED"
    });
  }

  assert.deepEqual(evaluateIdentityLogin({ signInProvider: "phone" }), {
    allowed: false,
    code: "PHONE_ONLY_LOGIN_NOT_ALLOWED"
  });
});

test("Kakao OIDC는 PoC 승인과 feature flag 전에는 닫혀 있다", () => {
  assert.deepEqual(evaluateIdentityLogin({ signInProvider: "oidc.kakao" }), {
    allowed: false,
    code: "PROVIDER_DISABLED"
  });
  assert.deepEqual(
    evaluateIdentityLogin({ signInProvider: "oidc.kakao", kakaoEnabled: true }),
    { allowed: true, code: "ALLOWED" }
  );
});

test("회원가입은 primary credential과 같은 UID에 link된 verified phone을 요구한다", () => {
  assert.deepEqual(evaluateEnrollmentPrincipal({
    linkedProviderIds: ["password", "phone"],
    verifiedPhone: true,
    emailVerified: true
  }), { allowed: true, code: "ALLOWED" });

  assert.deepEqual(evaluateEnrollmentPrincipal({
    linkedProviderIds: ["phone"],
    verifiedPhone: true,
    emailVerified: false
  }), { allowed: false, code: "PRIMARY_CREDENTIAL_REQUIRED" });

  assert.deepEqual(evaluateEnrollmentPrincipal({
    linkedProviderIds: ["password", "phone"],
    verifiedPhone: true,
    emailVerified: false
  }), { allowed: false, code: "EMAIL_VERIFICATION_REQUIRED" });
});

test("Firebase 교환과 고위험 경로는 revoke와 recent-auth를 검사한다", () => {
  assert.deepEqual(verificationPolicy("LOGIN_EXCHANGE"), {
    checkRevoked: true,
    maxAuthenticationAgeSeconds: 900,
    requiresRemoteFirebaseCall: true
  });

  for (const purpose of [
    "DIRECT_ENROLLMENT",
    "GUEST_ENROLLMENT",
    "GUEST_MERGE",
    "AUTH_METHOD_SYNC",
    "HIGH_RISK_REAUTHENTICATION"
  ]) {
    assert.deepEqual(verificationPolicy(purpose), {
      checkRevoked: true,
      maxAuthenticationAgeSeconds: 300,
      requiresRemoteFirebaseCall: true
    });
  }
});

test("Identity reissue와 Learning Core 요청은 Firebase를 호출하지 않는다", () => {
  assert.equal(
    NON_FIREBASE_PATH_POLICIES.IDENTITY_REFRESH_SESSION_REISSUE.callsFirebase,
    false
  );
  assert.equal(NON_FIREBASE_PATH_POLICIES.LEARNING_CORE_REQUEST.callsFirebase, false);
});

test("같은 Firebase binding은 같은 active enrollment 경합 키를 사용한다", () => {
  const first = enrollmentBinding({
    firebaseProjectId: "demo-project",
    firebaseUid: "opaque-firebase-uid",
    bindingType: "DIRECT_SIGNUP"
  });
  const retry = enrollmentBinding({
    firebaseProjectId: "demo-project",
    firebaseUid: "opaque-firebase-uid",
    bindingType: "DIRECT_SIGNUP"
  });

  assert.deepEqual(retry, first);
  assert.throws(
    () => enrollmentBinding({
      firebaseProjectId: "demo-project",
      firebaseUid: "opaque-firebase-uid",
      bindingType: "GUEST_USER"
    }),
    /INVALID_BOUNDUSERID/
  );
});

test("Guest merge는 source Guest JWT와 target Firebase proof를 모두 요구한다", () => {
  const result = resolveGuestMergeProof({
    guestJwtSubject: "source-user",
    sourceUser: {
      userId: "source-user",
      accountType: "GUEST",
      status: "ACTIVE"
    },
    firebasePrincipalProjectId: "demo-project",
    firebasePrincipalUid: "target-firebase-uid",
    firebaseIdentityOwner: {
      firebaseProjectId: "demo-project",
      firebaseUid: "target-firebase-uid",
      userId: "target-user",
      accountType: "MEMBER",
      status: "ACTIVE"
    }
  });

  assert.deepEqual(result, {
    sourceUserId: "source-user",
    targetUserId: "target-user"
  });

  assert.throws(
    () => resolveGuestMergeProof({
      guestJwtSubject: "different-user",
      sourceUser: {
        userId: "source-user",
        accountType: "GUEST",
        status: "ACTIVE"
      },
      firebasePrincipalProjectId: "demo-project",
      firebasePrincipalUid: "target-firebase-uid",
      firebaseIdentityOwner: {
        firebaseProjectId: "demo-project",
        firebaseUid: "target-firebase-uid",
        userId: "target-user",
        accountType: "MEMBER",
        status: "ACTIVE"
      }
    }),
    /INVALID_SOURCE_GUEST_PROOF/
  );

  assert.throws(
    () => resolveGuestMergeProof({
      guestJwtSubject: "source-user",
      sourceUser: {
        userId: "source-user",
        accountType: "GUEST",
        status: "ACTIVE"
      },
      firebasePrincipalProjectId: "different-project",
      firebasePrincipalUid: "target-firebase-uid",
      firebaseIdentityOwner: {
        firebaseProjectId: "demo-project",
        firebaseUid: "target-firebase-uid",
        userId: "target-user",
        accountType: "MEMBER",
        status: "ACTIVE"
      }
    }),
    /INVALID_TARGET_FIREBASE_PROOF/
  );
});
