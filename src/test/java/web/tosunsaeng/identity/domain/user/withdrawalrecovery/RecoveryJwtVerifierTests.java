package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.time.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class RecoveryJwtVerifierTests {
    static RSAKey key;
    static final String ISS = "urn:fixture:billing:test", AUD = "fixture:identity:recovery:test";
    static final RecoveryCursor.Owner OWNER = new RecoveryCursor.Owner("test", "billing");
    static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
    static final int PORT = 19443;
    @BeforeAll static void keys() throws Exception { key = new RSAKeyGenerator(2048).keyID("fixture-1").generate(); }
    String jwks() { return new JWKSet(key.toPublicJWK()).toString(); }
    RecoveryJwtVerifier verifier() { return new RecoveryJwtVerifier(jwks(), ISS, AUD, OWNER, CLOCK, PORT); }
    String claims() { return "{\"iss\":\"" + ISS + "\",\"sub\":\"billing-service\",\"aud\":\"" + AUD
            + "\",\"iat\":1000,\"exp\":1060,\"jti\":\"00000000-0000-4000-8000-000000000001\"}"; }
    String sign(String claims) throws Exception {
        var jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), new Payload(claims));
        jws.sign(new RSASSASigner(key)); return jws.serialize();
    }
    MockHttpServletRequest request(String claims) throws Exception {
        var r = new MockHttpServletRequest("POST", RecoveryProtocol.Route.CREATE.path());
        r.setAttribute(RecoveryWorkloadConfiguration.CONNECTOR_PORT, PORT);
        r.addHeader("Authorization", "Bearer " + sign(claims)); return r;
    }
    RecoveryCursor.Owner authorize(RecoveryJwtVerifier v, MockHttpServletRequest r) {
        return v.authorizeHeaders(r, RecoveryProtocol.Route.CREATE);
    }
    @Test void verifiesRealSignatureAndMapsOnlyConfiguredOwner() throws Exception {
        var r = request(claims()); r.addHeader("X-Consumer", "attacker"); r.addHeader("X-Environment", "production");
        assertThat(authorize(verifier(), r)).isEqualTo(OWNER);
    }
    @ParameterizedTest @ValueSource(strings = {"\"1000\"", "1000.0", "1e3", "null", "-1", "9223372036854775808"})
    void rejectsNonIntegerOrInvalidNumericDate(String value) throws Exception {
        var r = request(claims().replace("\"iat\":1000", "\"iat\":" + value));
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @ParameterizedTest @ValueSource(strings = {"\"exp\":1000", "\"exp\":1061", "\"iat\":1031", "\"nbf\":999", "\"nbf\":1060", "\"nbf\":1031"})
    void rejectsTimeRelations(String field) throws Exception {
        String body = field.startsWith("\"nbf") ? claims().replace("}", "," + field + "}")
                : claims().replaceAll(field.startsWith("\"exp") ? "\"exp\":1060" : "\"iat\":1000", field);
        var r = request(body);
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void expirySkewBoundaryIsExclusive() throws Exception {
        var r = request(claims());
        assertThat(authorize(new RecoveryJwtVerifier(jwks(), ISS, AUD, OWNER, Clock.fixed(Instant.ofEpochSecond(1089), ZoneOffset.UTC), PORT), r)).isEqualTo(OWNER);
        assertThatThrownBy(() -> authorize(new RecoveryJwtVerifier(jwks(), ISS, AUD, OWNER, Clock.fixed(Instant.ofEpochSecond(1090), ZoneOffset.UTC), PORT), r))
                .isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void futureAndNbfSkewBoundaryIsInclusive() throws Exception {
        var r = request(claims().replace("\"iat\":1000", "\"iat\":1030").replace("}", ",\"nbf\":1030}"));
        assertThat(authorize(verifier(), r)).isEqualTo(OWNER);
    }
    @ParameterizedTest @ValueSource(strings = {"billing-service", "fixture:identity:recovery:test"})
    void signedWrongSubjectOrPurposeIsForbidden(String value) throws Exception {
        var r = request(claims().replace(value, "other"));
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(SecurityException.class);
    }
    @Test void arrayAudienceAndDuplicateClaimsAreUnauthorized() throws Exception {
        for (String c : new String[]{claims().replace("\"aud\":\"" + AUD + "\"", "\"aud\":[\"" + AUD + "\"]"),
                claims().replace("}", ",\"iat\":1000}"), claims() + "{}", claims().replace(ISS, "other")}) {
            var r = request(c);
            assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
        }
    }
    @Test void duplicateOrMissingHeaderIsUnauthorized() throws Exception {
        var r = request(claims()); r.addHeader("Authorization", r.getHeader("Authorization"));
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
        r.removeHeader("Authorization");
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void legacyAwsAuthorizationIsNotAcceptedAsJwt() throws Exception {
        var r = request(claims()); r.removeHeader("Authorization");
        r.addHeader("Authorization", "AWS4-HMAC-SHA256 synthetic-legacy-authorization");
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void spoofedForwardingCannotSelectWorkloadConnector() throws Exception {
        var r = request(claims()); r.removeAttribute(RecoveryWorkloadConfiguration.CONNECTOR_PORT);
        r.setLocalPort(PORT); r.addHeader("X-Forwarded-Port", String.valueOf(PORT));
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(SecurityException.class);
    }
    @Test void privateOrDuplicateKeysFailStartupWithoutLeakingInput() {
        for (String value : new String[]{key.toJSONString(), "{\"keys\":[" + key.toJSONString() + "]}",
                "{\"keys\":[" + key.toPublicJWK().toJSONString() + "," + key.toPublicJWK().toJSONString() + "]}"})
            assertThatThrownBy(() -> new RecoveryJwtVerifier(value, ISS, AUD, OWNER, CLOCK, PORT))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid recovery workload trust configuration.").hasNoCause();
    }
    @Test void forgedSignatureAndRemovedKeyAreUnauthorized() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).keyID(key.getKeyID()).generate();
        var r = request(claims());
        var v = new RecoveryJwtVerifier(new JWKSet(other.toPublicJWK()).toString(), ISS, AUD, OWNER, CLOCK, PORT);
        assertThatThrownBy(() -> authorize(v, r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
        var removed = new RecoveryJwtVerifier(new JWKSet(new RSAKey.Builder(other).keyID("new").build().toPublicJWK()).toString(), ISS, AUD, OWNER, CLOCK, PORT);
        assertThatThrownBy(() -> authorize(removed, r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void portBoundaryPermitsOnlyExactWorkloadPosts() {
        for (var route : RecoveryProtocol.Route.values()) {
            assertThat(RecoveryWorkloadConfiguration.PortBoundary.allowed(PORT, PORT, "POST", route.path())).isTrue();
            assertThat(RecoveryWorkloadConfiguration.PortBoundary.allowed(8081, PORT, "POST", route.path())).isFalse();
            assertThat(RecoveryWorkloadConfiguration.PortBoundary.allowed(PORT, PORT, "GET", route.path())).isFalse();
        }
        for (String path : new String[]{"/auth/login", "/actuator/health", "/swagger-ui/index.html", RecoveryProtocol.PREFIX + "/other"})
            assertThat(RecoveryWorkloadConfiguration.PortBoundary.allowed(PORT, PORT, "POST", path)).isFalse();
        assertThat(RecoveryWorkloadConfiguration.PortBoundary.allowed(8081, PORT, "GET", "/actuator/health")).isTrue();
    }
    @Test void rotationOverlapAcceptsOldAndNewAndRevocationRejectsOld() throws Exception {
        RSAKey fresh = new RSAKeyGenerator(2048).keyID("fixture-2").generate();
        var verifier = new RecoveryJwtVerifier(new JWKSet(java.util.List.of(key.toPublicJWK(), fresh.toPublicJWK())).toString(), ISS, AUD, OWNER, CLOCK, PORT);
        assertThat(authorize(verifier, request(claims()))).isEqualTo(OWNER);
        var token = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("fixture-2").build(), new Payload(claims()));
        token.sign(new RSASSASigner(fresh)); var r = request(claims()); r.removeHeader("Authorization");
        r.addHeader("Authorization", "Bearer " + token.serialize());
        assertThat(authorize(verifier, r)).isEqualTo(OWNER);
        var newOnly = new RecoveryJwtVerifier(new JWKSet(fresh.toPublicJWK()).toString(), ISS, AUD, OWNER, CLOCK, PORT);
        var oldRequest = request(claims());
        assertThatThrownBy(() -> authorize(newOnly, oldRequest)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
        assertThat(authorize(newOnly, r)).isEqualTo(OWNER);
    }
    @ParameterizedTest @ValueSource(strings = {
            "{\"alg\":\"none\",\"kid\":\"fixture-1\"}",
            "{\"alg\":\"HS256\",\"kid\":\"fixture-1\"}",
            "{\"alg\":\"RS256\",\"kid\":\"fixture-1\",\"kid\":\"fixture-1\"}",
            "{\"alg\":\"RS256\",\"kid\":\"fixture-1\",\"jku\":\"https://invalid.example/jwks\"}",
            "{\"alg\":\"RS256\",\"kid\":\"fixture-1\",\"crit\":[\"other\"]}"})
    void untrustedHeaderCannotSelectAlgorithmOrRemoteKey(String header) throws Exception {
        var r = request(claims()); String[] parts = r.getHeader("Authorization").substring(7).split("\\.");
        r.removeHeader("Authorization");
        r.addHeader("Authorization", "Bearer " + java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "." + parts[1] + "." + parts[2]);
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void invalidUtf8PayloadAndOversizedHeaderAreUnauthorized() throws Exception {
        var r = request(claims()); String[] parts = r.getHeader("Authorization").substring(7).split("\\.");
        r.removeHeader("Authorization"); r.addHeader("Authorization", "Bearer " + parts[0] + ".wyg." + parts[2]);
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
        r.removeHeader("Authorization"); r.addHeader("Authorization", "Bearer " + "a".repeat(8192) + ".b.c");
        assertThatThrownBy(() -> authorize(verifier(), r)).isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
    }
    @Test void sharedTimeCases() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try (var input = getClass().getResourceAsStream("/contracts/payment-lifecycle/workload-jwt-r1/time-cases.json")) {
            var cases = mapper.readTree(input); assertThat(cases.size()).isEqualTo(16);
            for (var example : cases) {
                var body = (com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(claims());
                body.set("iat", example.get("iat")); body.set("exp", example.get("exp"));
                if (example.has("nbf")) body.set("nbf", example.get("nbf"));
                var r = request(body.toString());
                var v = new RecoveryJwtVerifier(jwks(), ISS, AUD, OWNER,
                        Clock.fixed(Instant.ofEpochSecond(example.path("now").longValue()), ZoneOffset.UTC), PORT);
                if (example.path("accepted").booleanValue()) assertThat(authorize(v, r)).as(example.path("name").textValue()).isEqualTo(OWNER);
                else assertThatThrownBy(() -> authorize(v, r)).as(example.path("name").textValue())
                        .isExactlyInstanceOf(RecoveryIngressVerifier.Unauthenticated.class);
            }
        }
    }
}
