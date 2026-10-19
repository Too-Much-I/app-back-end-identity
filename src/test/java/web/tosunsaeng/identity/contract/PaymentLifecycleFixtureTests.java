package web.tosunsaeng.identity.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** TMI-199 independent test oracle. Not an implemented recovery endpoint or Billing decoder. */
class PaymentLifecycleFixtureTests {
    static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    static byte[] bytes(String name) throws Exception {
        try (var in = PaymentLifecycleFixtureTests.class.getResourceAsStream(
                "/contracts/payment-lifecycle/v1/" + name + ".json")) {
            if (in == null) throw new IllegalStateException("Missing fixture " + name);
            return in.readAllBytes();
        }
    }

    static JsonNode fixture(String name) throws Exception { return JSON.readTree(bytes(name)); }
    static Stream<JsonNode> cases(String name) throws Exception {
        List<JsonNode> cases = new ArrayList<>();
        fixture("protocol-cases").get(name).forEach(cases::add);
        return cases.stream();
    }
    static Stream<JsonNode> decoderCases() throws Exception { return cases("decoderCases"); }
    static Stream<JsonNode> ackCases() throws Exception { return cases("ackCases"); }
    static Stream<JsonNode> feedCases() throws Exception { return cases("feedCases"); }
    static Stream<JsonNode> httpCases() throws Exception { return cases("httpCases"); }

    @Test
    void copiedFixturesAreByteIdenticalToReviewedBillingInputs() throws Exception {
        Map<String, String> hashes = Map.of(
                "snapshot-digest", "bd8606d7d5304b2a59399c1c1eeb68a2ef39b1cf9abcb2bb4dbd18fec0a4582d",
                "protocol-cases", "d7238d963ef786f3304e0e00cc3d677af66b85700f2aa96557111ab905a5cab1",
                "wire-examples", "6c1f4611373d08b65196998a2d6ad1d37da03414b2a1b4e8c761d1765860fb1b",
                "billing-indexes", "6395954e0c7ea62d050c1b0bdcb3bd8887a00721adcee7876a1bbc62ff834aff");
        for (var entry : hashes.entrySet()) {
            assertThat(sha256(bytes(entry.getKey()))).isEqualTo(entry.getValue());
            assertThat(fixture(entry.getKey()).path("fixtureVersion").asInt()).isEqualTo(1);
        }
    }

    static String sha256(byte[] value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    }

    record Row(String userId, String withdrawnAt) { }

    static String digest(List<List<Row>> pages) throws Exception {
        var bytes = new ByteArrayOutputStream();
        var output = new DataOutputStream(bytes);
        String previous = "";
        for (var page : pages) for (var row : page) {
            if (!uuid(row.userId()) || previous.compareTo(row.userId()) >= 0)
                throw new IllegalArgumentException("Noncanonical user order");
            // A typed record, rather than Billing's ObjectNode serializer.
            byte[] encoded = JSON.writeValueAsBytes(new Row(row.userId(),
                    Instant.parse(row.withdrawnAt()).toString()));
            output.writeInt(encoded.length);
            output.write(encoded);
            previous = row.userId();
        }
        return sha256(bytes.toByteArray());
    }

    @Test
    void digestMatchesGoldenIncludingNanosEmptyPagesAndOrdering() throws Exception {
        var source = fixture("snapshot-digest");
        List<Row> rows = new ArrayList<>();
        for (var row : source.get("canonicalRows"))
            rows.add(new Row(row.get("userId").asText(), row.get("withdrawnAt").asText()));
        assertThat(digest(List.of(rows))).isEqualTo(source.get("expectedDigest").asText());
        assertThat(digest(List.of(rows.subList(0, 1), List.of(), rows.subList(1, 4))))
                .isEqualTo(source.get("expectedDigest").asText());
        assertThat(digest(List.of())).isEqualTo(source.get("expectedEmptyDigest").asText());
        assertThatThrownBy(() -> digest(List.of(List.of(rows.get(1), rows.get(0)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> digest(List.of(List.of(rows.get(0)), List.of(rows.get(0)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    static boolean uuid(String value) {
        try { return UUID.fromString(value).toString().equals(value); }
        catch (RuntimeException invalid) { return false; }
    }

    static String decode(String body) {
        try {
            if (body.getBytes(StandardCharsets.UTF_8).length > 4096) return "INVALID_REQUEST";
            var node = JSON.readTree(body);
            if (!node.isObject() || node.size() != 4) return "INVALID_REQUEST";
            if (!node.path("eventId").isTextual() || !node.path("userId").isTextual()
                    || !node.path("withdrawnAt").isTextual()
                    || !node.path("schemaVersion").isIntegralNumber()) return "INVALID_REQUEST";
            if (!uuid(node.get("eventId").textValue()) || !uuid(node.get("userId").textValue()))
                return "INVALID_REQUEST";
            Instant.parse(node.get("withdrawnAt").textValue());
            return node.get("schemaVersion").bigIntegerValue().equals(BigInteger.ONE)
                    ? "ACCEPT" : "UNSUPPORTED_CONTRACT";
        } catch (Exception invalid) { return "INVALID_REQUEST"; }
    }

    @ParameterizedTest
    @MethodSource("decoderCases")
    void independentStrictDecoderMatchesSharedCases(JsonNode test) throws Exception {
        ObjectNode event = fixture("protocol-cases").get("withdrawal").deepCopy();
        String mutation = test.get("mutation").asText();
        switch (mutation) {
            case "UNKNOWN_FIELD" -> event.put("extra", true);
            case "STRING_VERSION" -> event.put("schemaVersion", "1");
            case "VERSION_TWO" -> event.put("schemaVersion", 2);
            case "UPPERCASE_ID" -> event.put("eventId", event.get("eventId").asText().toUpperCase(java.util.Locale.ROOT));
            case "MISSING_USER" -> event.remove("userId");
            default -> { }
        }
        String body = JSON.writeValueAsString(event);
        if (mutation.equals("TRAILING_TOKEN")) body += " {}";
        if (mutation.equals("DUPLICATE_FIELD")) body = body.replaceFirst("\\{", "{\"schemaVersion\":1,");
        assertThat(decode(body)).as(test.get("name").asText()).isEqualTo(test.get("expected").asText());
    }

    @ParameterizedTest
    @MethodSource("ackCases")
    void generationFencePrecedesAnyAckNoop(JsonNode test) {
        String result = "CONFLICT";
        if (!test.get("sameGeneration").asBoolean()) result = "RECOVERY_GENERATION_MISMATCH";
        else if (test.get("mode").asText().equals("FEED")) {
            if (test.get("current").isNull()) result = "RECOVERY_NOT_READY";
            else result = test.get("target").bigIntegerValue().compareTo(test.get("current").bigIntegerValue()) <= 0
                    ? "NOOP" : "APPLIED";
        } else if (test.get("current").isNull() && test.get("digestMatches").asBoolean()
                && test.get("target").equals(test.get("snapshotH"))) result = "APPLIED";
        assertThat(result).as(test.get("name").asText()).isEqualTo(test.get("expected").asText());
    }

    @ParameterizedTest
    @MethodSource("feedCases")
    void fixedFeedDoesNotSkipGaps(JsonNode test) {
        BigInteger after = test.get("after").bigIntegerValue();
        BigInteger target = test.get("target").bigIntegerValue();
        BigInteger through = after;
        String result = after.compareTo(test.get("floor").bigIntegerValue()) < 0 ? "REPLAY_UNAVAILABLE" : null;
        for (var item : test.get("items")) {
            if (result != null) break;
            var next = item.bigIntegerValue();
            if (next.compareTo(target) > 0) result = "INVALID_PAGE";
            else if (!next.equals(through.add(BigInteger.ONE))) result = "COVERAGE_GAP";
            else through = next;
        }
        if (result == null) result = through.equals(target) ? "DONE" : through.equals(after) ? "COVERAGE_GAP" : "MORE";
        assertThat(result).as(test.get("name").asText()).isEqualTo(test.get("expected").asText());
    }

    @ParameterizedTest
    @MethodSource("httpCases")
    void billingAckPolicyIsNotExistingLcPublisherPolicy(JsonNode test) {
        int status = test.get("status").asInt();
        String result = switch (status) {
            case 204 -> "DELIVERED";
            case 401, 403 -> "BLOCKED_AUTH";
            case 408, 425, 429 -> "RETRY";
            default -> status >= 500 && status <= 599 ? "RETRY" : "DEAD_LETTER";
        };
        assertThat(result).isEqualTo(test.get("expected").asText());
    }

    static BigInteger decimal(JsonNode node) {
        if (!node.isTextual() || !node.textValue().matches("0|[1-9][0-9]*"))
            throw new IllegalArgumentException("Expected canonical decimal string");
        return new BigInteger(node.textValue());
    }

    @Test
    void proposedWireUsesDecimalStringsBsonTimestampAndRequiredNull() throws Exception {
        var wire = fixture("wire-examples");
        var manifest = wire.get("snapshotManifest");
        for (String field : List.of("H", "itemCount", "pageCount"))
            assertThat(decimal(manifest.get(field)).signum()).isNotNegative();
        for (String field : List.of("seconds", "increment"))
            assertThat(decimal(manifest.get("T").get(field)))
                    .isBetween(BigInteger.ZERO, new BigInteger("4294967295"));
        assertThat(wire.get("baselineAck").has("expectedCheckpointVersion")).isTrue();
        assertThat(wire.get("baselineAck").get("expectedCheckpointVersion").isNull()).isTrue();
        assertThat(wire.get("baselineAck").get("contentDigest")).isEqualTo(manifest.get("contentDigest"));
        assertThat(wire.get("baselineAck").get("throughSequence")).isEqualTo(manifest.get("H"));
        assertThat(manifest.get("consumerRecoveryGeneration"))
                .isEqualTo(wire.get("generationHeader").get("X-Consumer-Recovery-Generation"));
        for (String invalid : List.of("01", "-1", "1.0", "+1", " 1"))
            assertThatThrownBy(() -> decimal(JSON.getNodeFactory().textNode(invalid)))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> decimal(JSON.getNodeFactory().numberNode(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(decimal(JSON.getNodeFactory().textNode("10")))
                .isGreaterThan(decimal(JSON.getNodeFactory().textNode("9")));
    }
}
