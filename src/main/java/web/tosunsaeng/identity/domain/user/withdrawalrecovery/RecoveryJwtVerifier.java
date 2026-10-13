package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.Clock;
import java.util.*;

/** Dedicated deployed public keys; no remote key lookup, user-token decoder or algorithm fallback. */
public final class RecoveryJwtVerifier implements RecoveryIngressVerifier {
    public static final int MAX_AUTHORIZATION = 8192;
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private final Map<String, RSAKey> keys;
    private final String issuer, audience;
    private final RecoveryCursor.Owner owner;
    private final Clock clock;
    private final int port;

    public RecoveryJwtVerifier(String publicJwks, String issuer, String audience,
            RecoveryCursor.Owner owner, Clock clock, int port) {
        try {
            if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()
                    || owner == null || clock == null || port < 1 || port > 65535
                    || publicJwks == null || publicJwks.length() > 65536) throw new IllegalArgumentException();
            var root = JSON.readTree(publicJwks);
            if (!root.isObject() || root.size() != 1 || !root.path("keys").isArray()
                    || root.path("keys").isEmpty() || root.path("keys").size() > 8) throw new IllegalArgumentException();
            var parsed = new HashMap<String, RSAKey>();
            for (var node : root.path("keys")) {
                var fields = node.fieldNames();
                while (fields.hasNext()) if (!Set.of("kty", "kid", "n", "e", "alg", "use", "key_ops").contains(fields.next()))
                    throw new IllegalArgumentException();
                String kid = text(node, "kid");
                if (!"RSA".equals(text(node, "kty")) || kid.isBlank()
                        || (node.has("alg") && !"RS256".equals(text(node, "alg")))
                        || (node.has("use") && !"sig".equals(text(node, "use")))
                        || (node.has("key_ops") && !(node.path("key_ops").isArray()
                            && node.path("key_ops").size() == 1 && "verify".equals(node.path("key_ops").get(0).textValue()))))
                    throw new IllegalArgumentException();
                RSAKey key = RSAKey.parse(node.toString());
                if (key.isPrivate() || key.toRSAPublicKey().getModulus().bitLength() < 2048
                        || parsed.putIfAbsent(kid, key) != null) throw new IllegalArgumentException();
            }
            this.keys = Map.copyOf(parsed);
        } catch (Exception ignored) { throw new IllegalArgumentException("Invalid recovery workload trust configuration."); }
        this.issuer = issuer; this.audience = audience; this.owner = owner; this.clock = clock; this.port = port;
    }

    @Override public RecoveryCursor.Owner authorizeHeaders(HttpServletRequest request, RecoveryProtocol.Route route) {
        // The marker is set by the Tomcat valve using the actual connector, not proxy-derived localPort.
        if (!Integer.valueOf(port).equals(request.getAttribute(RecoveryWorkloadConfiguration.CONNECTOR_PORT))
                || !"POST".equals(request.getMethod()) || !route.path().equals(request.getRequestURI()))
            throw new SecurityException("Workload route rejected.");
        long now = clock.instant().getEpochSecond(); // Clock failure remains a 503, never credential rejection.
        JsonNode claims;
        try {
            var headers = Collections.list(request.getHeaders("Authorization"));
            if (headers.size() != 1) throw new IllegalArgumentException();
            String value = headers.getFirst();
            if (value.length() > MAX_AUTHORIZATION || !value.matches("(?i:Bearer) [A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"))
                throw new IllegalArgumentException();
            String token = value.substring(7);
            String[] parts = token.split("\\.");
            JsonNode header = decode(parts[0]); claims = decode(parts[1]);
            var names = header.fieldNames();
            while (names.hasNext()) if (!Set.of("alg", "kid", "typ").contains(names.next())) throw new IllegalArgumentException();
            if (!"RS256".equals(text(header, "alg")) || (header.has("typ") && !"JWT".equals(text(header, "typ"))))
                throw new IllegalArgumentException();
            RSAKey key = keys.get(text(header, "kid"));
            if (key == null || !issuer.equals(text(claims, "iss"))
                    || !JWSObject.parse(token).verify(new RSASSAVerifier(key))) throw new IllegalArgumentException();
            text(claims, "sub"); text(claims, "aud");
            RecoveryProtocol.uuid4(text(claims, "jti"));
            BigInteger iat = time(claims, "iat"), exp = time(claims, "exp"), n = BigInteger.valueOf(now);
            BigInteger ttl = exp.subtract(iat), skew = BigInteger.valueOf(30);
            if (ttl.signum() <= 0 || ttl.compareTo(BigInteger.valueOf(60)) > 0
                    || iat.compareTo(n.add(skew)) > 0 || n.compareTo(exp.add(skew)) >= 0) throw new IllegalArgumentException();
            if (claims.has("nbf")) {
                BigInteger nbf = time(claims, "nbf");
                if (nbf.compareTo(iat) < 0 || nbf.compareTo(exp) >= 0 || nbf.compareTo(n.add(skew)) > 0)
                    throw new IllegalArgumentException();
            }
        } catch (Exception ignored) { throw new Unauthenticated(); }
        if (!"billing-service".equals(text(claims, "sub")) || !audience.equals(text(claims, "aud")))
            throw new SecurityException("Workload permission rejected.");
        return owner;
    }

    private static JsonNode decode(String part) throws Exception {
        byte[] bytes = Base64.getUrlDecoder().decode(part);
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(part)) throw new IllegalArgumentException();
        String raw = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        JsonNode value = JSON.readTree(raw);
        if (value == null || !value.isObject()) throw new IllegalArgumentException();
        return value;
    }
    private static String text(JsonNode node, String name) {
        if (!node.path(name).isTextual()) throw new IllegalArgumentException();
        return node.path(name).textValue();
    }
    private static BigInteger time(JsonNode node, String name) {
        JsonNode value = node.path(name);
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) throw new IllegalArgumentException();
        return value.bigIntegerValue();
    }
}
