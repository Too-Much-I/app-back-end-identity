package web.tosunsaeng.identity.global.workload;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static web.tosunsaeng.identity.global.workload.BillingJsonTransport.*;

/** HTTPS origin pinned in deployment configuration; every attempt gets a fresh credential. */
public final class BillingJwtJsonTransport implements BillingJsonTransport {
    public static final Set<String> ROUTES = Set.of(
            "/internal/v1/payments/accounts/withdrawal/events", "/internal/v1/eligibility/trial/events",
            "/internal/v1/eligibility/trial/owner/events", "/internal/v1/owners/merge/events");
    public static final class Factory {
        private final RSAKey key;
        private final String issuer;
        private final Map<String, String> audiences;
        private final Clock clock;
        public Factory(String privateJwk, String issuer, Map<String, String> audiences, Clock clock) {
            try {
                if (privateJwk == null || privateJwk.length() > 16384 || issuer == null || issuer.isBlank() || clock == null
                        || audiences == null || audiences.isEmpty() || !ROUTES.containsAll(audiences.keySet())
                        || audiences.values().stream().anyMatch(v -> v == null || v.isBlank())
                        || new HashSet<>(audiences.values()).size() != audiences.size()) throw new IllegalArgumentException();
                var node = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(privateJwk);
                if (node == null || !node.isObject()) throw new IllegalArgumentException();
                var names = node.fieldNames();
                while (names.hasNext()) if (!Set.of("kty", "kid", "n", "e", "d", "p", "q", "dp", "dq", "qi", "alg", "use", "key_ops").contains(names.next()))
                    throw new IllegalArgumentException();
                key = RSAKey.parse(node.toString());
                if (!key.isPrivate() || key.getKeyID() == null || key.getKeyID().isBlank()
                        || key.toRSAPublicKey().getModulus().bitLength() < 2048
                        || (key.getAlgorithm() != null && !JWSAlgorithm.RS256.equals(key.getAlgorithm()))
                        || (key.getKeyUse() != null && !"sig".equals(key.getKeyUse().identifier()))
                        || (key.getKeyOperations() != null && !key.getKeyOperations().equals(Set.of(com.nimbusds.jose.jwk.KeyOperation.SIGN))))
                    throw new IllegalArgumentException();
                // Verify the configured private/public pair before accepting startup.
                var proof = new JWSObject(new JWSHeader(JWSAlgorithm.RS256), new Payload("startup-self-check"));
                proof.sign(new RSASSASigner(key));
                if (!proof.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(key.toPublicJWK()))) throw new IllegalArgumentException();
            } catch (Exception ignored) { throw new IllegalArgumentException("Invalid Billing workload signing configuration."); }
            this.issuer = issuer; this.audiences = Map.copyOf(audiences); this.clock = clock;
        }
        public BillingJsonTransport create(URI origin, Duration connect, Duration read) {
            return new BillingJwtJsonTransport(this, origin, read, HttpClient.newBuilder()
                    .connectTimeout(connect).followRedirects(HttpClient.Redirect.NEVER).build());
        }
        String issue(String route) {
            try {
                String audience = audiences.get(route);
                if (audience == null) throw new IllegalArgumentException();
                long now = clock.instant().getEpochSecond();
                if (now < 0) throw new IllegalStateException();
                String body = new ObjectMapper().writeValueAsString(Map.of("iss", issuer, "sub", "identity-service",
                        "aud", audience, "iat", now, "exp", Math.addExact(now, 60), "jti", UUID.randomUUID().toString()));
                var jws = new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).type(JOSEObjectType.JWT).build(), new Payload(body));
                jws.sign(new RSASSASigner(key)); return jws.serialize();
            } catch (Exception ignored) { throw failure(FailureKind.CREDENTIAL_UNAVAILABLE); }
        }
    }
    private final Factory factory;
    private final URI origin;
    private final Duration read;
    private final HttpClient client;
    BillingJwtJsonTransport(Factory factory, URI origin, Duration read, HttpClient client) {
        if (origin == null || !"https".equals(origin.getScheme()) || origin.getHost() == null
                || origin.getUserInfo() != null || origin.getQuery() != null || origin.getFragment() != null
                || !(origin.getPath().isEmpty() || origin.getPath().equals("/"))
                || read == null || read.isNegative() || read.isZero() || client.followRedirects() != HttpClient.Redirect.NEVER)
            throw new IllegalArgumentException("Billing workload requires a pinned HTTPS origin and no redirects.");
        this.factory = Objects.requireNonNull(factory); this.origin = URI.create("https://" + origin.getRawAuthority());
        this.read = read; this.client = client;
    }
    @Override public DetailedResult postDetailed(String route, byte[] body) {
        if (!ROUTES.contains(route)) throw new IllegalArgumentException("Unsupported Billing workload route.");
        byte[] bytes = Objects.requireNonNull(body).clone();
        var request = HttpRequest.newBuilder(URI.create(origin + route)).timeout(read)
                .header("Content-Type", "application/json").header("Authorization", "Bearer " + factory.issue(route))
                .header("traceparent", "00-" + UUID.randomUUID().toString().replace("-", "") + "-"
                        + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "-01")
                .POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return new DetailedResult(response.statusCode(), response.headers().allValues("Retry-After").stream()
                    .limit(2).map(v -> v.length() > 128 ? "INVALID" : v).toList());
        } catch (HttpTimeoutException ignored) { throw failure(FailureKind.TIMEOUT); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); throw failure(FailureKind.CONNECTION); }
        catch (java.io.IOException ignored) { throw failure(FailureKind.CONNECTION); }
    }
    private static TransportException failure(FailureKind kind) {
        return new TransportException(kind, new IllegalStateException("Billing workload transport failed."));
    }
}
