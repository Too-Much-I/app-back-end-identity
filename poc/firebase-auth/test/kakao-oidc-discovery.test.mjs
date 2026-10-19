import assert from "node:assert/strict";
import test from "node:test";

const DISCOVERY_URL = "https://kauth.kakao.com/.well-known/openid-configuration";

test("Kakao 공개 OIDC discovery는 Generic OIDC 후보의 최소 계약을 제공한다", async () => {
  const response = await fetch(DISCOVERY_URL, {
    signal: AbortSignal.timeout(10_000)
  });
  assert.equal(response.ok, true);

  const discovery = await response.json();
  assert.equal(discovery.issuer, "https://kauth.kakao.com");
  assert.equal(typeof discovery.authorization_endpoint, "string");
  assert.equal(typeof discovery.token_endpoint, "string");
  assert.equal(typeof discovery.jwks_uri, "string");
  assert.equal(discovery.response_types_supported.includes("code"), true);
  assert.equal(discovery.grant_types_supported.includes("authorization_code"), true);
  assert.equal(discovery.subject_types_supported.includes("pairwise"), true);
  assert.equal(discovery.id_token_signing_alg_values_supported.includes("RS256"), true);
  assert.equal(discovery.code_challenge_methods_supported.includes("S256"), true);
  assert.equal(discovery.claims_supported.includes("sub"), true);
});

