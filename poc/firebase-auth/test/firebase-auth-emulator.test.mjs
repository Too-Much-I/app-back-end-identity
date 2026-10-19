import assert from "node:assert/strict";
import { randomUUID } from "node:crypto";
import { after, afterEach, before, beforeEach, test } from "node:test";

import {
  deleteApp as deleteAdminApp,
  initializeApp as initializeAdminApp
} from "firebase-admin/app";
import { getAuth as getAdminAuth } from "firebase-admin/auth";
import {
  deleteApp as deleteClientApp,
  initializeApp as initializeClientApp
} from "firebase/app";
import {
  GoogleAuthProvider,
  OAuthProvider,
  connectAuthEmulator,
  createUserWithEmailAndPassword,
  getIdToken,
  inMemoryPersistence,
  initializeAuth,
  signInWithCredential,
  signInWithEmailAndPassword,
  signOut
} from "firebase/auth";

import { evaluateIdentityLogin } from "../src/identity-policy.mjs";

const PROJECT_ID = "demo-tosunsaeng-identity-poc";
const EMULATOR_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST;

if (!EMULATOR_HOST) {
  throw new Error("FIREBASE_AUTH_EMULATOR_HOST_REQUIRED");
}

const EMULATOR_ORIGIN = `http://${EMULATOR_HOST}`;
const clientApps = [];
let adminApp;
let adminAuth;

before(() => {
  assert.equal(process.env.GCLOUD_PROJECT, PROJECT_ID);
  adminApp = initializeAdminApp({ projectId: PROJECT_ID }, `poc-admin-${randomUUID()}`);
  adminAuth = getAdminAuth(adminApp);
});

beforeEach(async () => {
  await clearEmulatorAccounts();
});

afterEach(async () => {
  while (clientApps.length > 0) {
    await deleteClientApp(clientApps.pop());
  }
});

after(async () => {
  await deleteAdminApp(adminApp);
});

test("email/password 인증과 Admin ID Token 검증을 격리 emulator에서 수행한다", async () => {
  const auth = createClientAuth("email-password");
  const email = `${randomUUID()}@example.test`;
  const password = runtimeCredential();

  const created = await createUserWithEmailAndPassword(auth, email, password);
  const createdUid = created.user.uid;
  await signOut(auth);

  const signedIn = await signInWithEmailAndPassword(auth, email, password);
  assert.equal(signedIn.user.uid, createdUid);

  const idToken = await getIdToken(signedIn.user, true);
  const decoded = await adminAuth.verifyIdToken(idToken, true);

  assert.equal(decoded.uid, createdUid);
  assert.equal(decoded.aud, PROJECT_ID);
  assert.equal(decoded.iss, `https://securetoken.google.com/${PROJECT_ID}`);
  assert.equal(decoded.firebase.sign_in_provider, "password");
  assert.equal(decoded.email_verified, false);
  assert.equal(evaluateIdentityLogin({ signInProvider: decoded.firebase.sign_in_provider }).allowed, true);

  await adminAuth.updateUser(createdUid, { emailVerified: true });
  await signOut(auth);
  const verifiedEmailSignIn = await signInWithEmailAndPassword(auth, email, password);
  const verifiedEmailToken = await getIdToken(verifiedEmailSignIn.user, true);
  const verifiedEmailDecoded = await adminAuth.verifyIdToken(verifiedEmailToken, true);
  assert.equal(verifiedEmailDecoded.email_verified, true);

  await adminAuth.updateUser(createdUid, { disabled: true });
  await assert.rejects(
    () => adminAuth.verifyIdToken(verifiedEmailToken, true),
    error => error?.code === "auth/user-disabled"
  );
});

test("Admin revoke와 delete는 checkRevoked 검증에서 기존 ID Token을 차단한다", async () => {
  const auth = createClientAuth("revoke-delete");
  const created = await createUserWithEmailAndPassword(
    auth,
    `${randomUUID()}@example.test`,
    runtimeCredential()
  );
  const idToken = await getIdToken(created.user, true);

  await adminAuth.verifyIdToken(idToken, true);
  await new Promise(resolve => setTimeout(resolve, 1100));
  await adminAuth.revokeRefreshTokens(created.user.uid);

  await assert.rejects(
    () => adminAuth.verifyIdToken(idToken, true),
    error => error?.code === "auth/id-token-revoked"
  );

  await adminAuth.deleteUser(created.user.uid);
  await assert.rejects(
    () => adminAuth.getUser(created.user.uid),
    error => error?.code === "auth/user-not-found"
  );
});

test("Google과 Apple emulator credential은 stable provider UID로 다시 로그인한다", async () => {
  await assertStableFederatedIdentity({
    providerId: "google.com",
    credential: GoogleAuthProvider.credential(
      unsignedProviderToken("https://accounts.google.com", "google-subject")
    )
  });

  const appleProvider = new OAuthProvider("apple.com");
  await assertStableFederatedIdentity({
    providerId: "apple.com",
    credential: appleProvider.credential({
      idToken: unsignedProviderToken("https://appleid.apple.com", "apple-subject"),
      rawNonce: "poc-nonce"
    })
  });
});

test("phone credential link는 UID를 유지하고 phone-only 로그인은 정책으로 거절한다", async () => {
  const auth = createClientAuth("phone-link");
  const created = await createUserWithEmailAndPassword(
    auth,
    `${randomUUID()}@example.test`,
    runtimeCredential()
  );
  const uidBeforeLink = created.user.uid;
  const linked = await completePhoneVerification({
    phoneNumber: "+16505550101",
    idToken: await getIdToken(created.user, true)
  });
  assert.equal(linked.localId, uidBeforeLink);

  const refreshedToken = linked.idToken;
  const decoded = await adminAuth.verifyIdToken(refreshedToken, true);
  const record = await adminAuth.getUser(uidBeforeLink);
  assert.equal(decoded.uid, uidBeforeLink);
  assert.equal(decoded.firebase.sign_in_provider, "phone");
  assert.equal(record.providerData.some(data => data.providerId === "phone"), true);
  assert.equal(record.providerData.some(data => data.providerId === "password"), true);
  assert.equal(typeof record.phoneNumber, "string");

  await clearEmulatorAccounts();
  const phoneOnly = await completePhoneVerification({
    phoneNumber: "+16505550102"
  });
  const phoneOnlyDecoded = await adminAuth.verifyIdToken(phoneOnly.idToken, true);
  const phoneOnlyRecord = await adminAuth.getUser(phoneOnly.localId);

  assert.equal(phoneOnlyDecoded.firebase.sign_in_provider, "phone");
  assert.deepEqual(
    phoneOnlyRecord.providerData.map(data => data.providerId),
    ["phone"]
  );
  assert.deepEqual(
    evaluateIdentityLogin({ signInProvider: phoneOnlyDecoded.firebase.sign_in_provider }),
    { allowed: false, code: "PHONE_ONLY_LOGIN_NOT_ALLOWED" }
  );
});

test("다른 Firebase User가 점유한 phone은 충돌하고 owner 삭제 뒤 다시 link된다", async () => {
  const ownerAuth = createClientAuth("phone-owner");
  const owner = await createUserWithEmailAndPassword(
    ownerAuth,
    `${randomUUID()}@example.test`,
    runtimeCredential()
  );
  await completePhoneVerification({
    phoneNumber: "+16505550103",
    idToken: await getIdToken(owner.user, true)
  });

  const candidateAuth = createClientAuth("phone-candidate");
  const candidate = await createUserWithEmailAndPassword(
    candidateAuth,
    `${randomUUID()}@example.test`,
    runtimeCredential()
  );
  const conflict = await completePhoneVerification({
    phoneNumber: "+16505550103",
    idToken: await getIdToken(candidate.user, true),
    allowConflict: true
  });
  assert.equal(conflict.conflict, true);

  await adminAuth.deleteUser(owner.user.uid);

  const linkedAfterCleanup = await completePhoneVerification({
    phoneNumber: "+16505550103",
    idToken: await getIdToken(candidate.user, true)
  });
  assert.equal(linkedAfterCleanup.localId, candidate.user.uid);
});

async function assertStableFederatedIdentity({ providerId, credential }) {
  const firstAuth = createClientAuth(`${providerId}-first`);
  const first = await signInWithCredential(firstAuth, credential);
  const providerSubject = providerUid(first.user, providerId);
  const firstUid = first.user.uid;
  await signOut(firstAuth);

  const secondAuth = createClientAuth(`${providerId}-second`);
  const second = await signInWithCredential(secondAuth, credential);
  assert.equal(second.user.uid, firstUid);
  assert.equal(providerUid(second.user, providerId), providerSubject);

  const token = await getIdToken(second.user, true);
  const decoded = await adminAuth.verifyIdToken(token, true);
  assert.equal(decoded.firebase.sign_in_provider, providerId);
}

function providerUid(user, providerId) {
  const provider = user.providerData.find(data => data.providerId === providerId);
  assert.ok(provider);
  assert.equal(typeof provider.uid, "string");
  assert.notEqual(provider.uid.length, 0);
  return provider.uid;
}

function createClientAuth(label) {
  const app = initializeClientApp(
    {
      apiKey: "demo-api-key",
      authDomain: `${PROJECT_ID}.firebaseapp.com`,
      projectId: PROJECT_ID
    },
    `poc-client-${label}-${randomUUID()}`
  );
  clientApps.push(app);

  const auth = initializeAuth(app, { persistence: inMemoryPersistence });
  connectAuthEmulator(auth, EMULATOR_ORIGIN, { disableWarnings: true });
  return auth;
}

async function completePhoneVerification({ phoneNumber, idToken, allowConflict = false }) {
  const sendResponse = await identityToolkitRequest("accounts:sendVerificationCode", {
    phoneNumber,
    recaptchaToken: "poc-emulator-verifier"
  });
  const verificationId = sendResponse.sessionInfo;
  const response = await fetch(
    `${EMULATOR_ORIGIN}/emulator/v1/projects/${PROJECT_ID}/verificationCodes`
  );
  assert.equal(response.ok, true);
  const body = await response.json();
  const verification = body.verificationCodes.find(
    candidate => candidate.sessionInfo === verificationId
      || candidate.phoneNumber === phoneNumber
  );
  assert.ok(verification);

  const completion = await identityToolkitRequest(
    "accounts:signInWithPhoneNumber",
    {
      sessionInfo: verificationId,
      code: verification.code,
      idToken,
      returnSecureToken: true
    },
    allowConflict
  );
  if (completion.temporaryProof) {
    if (!allowConflict) {
      throw new Error("UNEXPECTED_PHONE_OWNERSHIP_CONFLICT");
    }
    return { conflict: true };
  }
  return completion;
}

async function identityToolkitRequest(operation, body, allowConflict = false) {
  const response = await fetch(
    `${EMULATOR_ORIGIN}/identitytoolkit.googleapis.com/v1/${operation}?key=demo-api-key`,
    {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body)
    }
  );
  const payload = await response.json();
  if (!response.ok) {
    if (allowConflict && isPhoneOwnershipConflict(payload)) {
      return { conflict: true };
    }
    throw new Error(`FIREBASE_EMULATOR_REQUEST_FAILED:${operation}`);
  }
  return payload;
}

function isPhoneOwnershipConflict(payload) {
  const message = payload?.error?.message;
  return typeof message === "string" && [
    "PHONE_NUMBER_EXISTS",
    "FEDERATED_USER_ID_ALREADY_LINKED",
    "CREDENTIAL_TOO_OLD_LOGIN_AGAIN"
  ].some(code => message.includes(code));
}

async function clearEmulatorAccounts() {
  const response = await fetch(
    `${EMULATOR_ORIGIN}/emulator/v1/projects/${PROJECT_ID}/accounts`,
    { method: "DELETE" }
  );
  assert.equal(response.ok, true);
}

function runtimeCredential() {
  return `Poc-${randomUUID()}-Aa1!`;
}

function unsignedProviderToken(issuer, subject) {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url({ alg: "none", typ: "JWT" });
  const payload = base64Url({
    iss: issuer,
    aud: "demo-client",
    sub: subject,
    iat: now,
    exp: now + 3600,
    email: `${subject}@example.test`,
    email_verified: true,
    nonce: "poc-nonce"
  });
  return `${header}.${payload}.`;
}

function base64Url(value) {
  return Buffer.from(JSON.stringify(value))
    .toString("base64url");
}
