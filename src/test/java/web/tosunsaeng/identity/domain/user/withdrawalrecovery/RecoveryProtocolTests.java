package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocol.Error;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocol.*;

class RecoveryProtocolTests {
    static final ObjectMapper JSON = new ObjectMapper();
    static final String ROOT = "/contracts/payment-lifecycle/recovery-r1/";
    static final String ID = "00000000-0000-4000-8000-000000000001";

    static JsonNode fixture(String file) throws Exception {
        try (var in = RecoveryProtocolTests.class.getResourceAsStream(ROOT + file)) {
            if (in == null) throw new IllegalStateException("Missing test fixture.");
            return JSON.readTree(in);
        }
    }
    static Stream<JsonNode> requests() throws Exception {
        List<JsonNode> values = new ArrayList<>(); fixture("request-cases.json").get("cases").forEach(values::add);
        return values.stream();
    }
    static Stream<JsonNode> states() throws Exception {
        List<JsonNode> values = new ArrayList<>(); fixture("snapshot-state-cases.json").get("cases").forEach(values::add);
        return values.stream();
    }

    @ParameterizedTest @MethodSource("requests")
    void strictDecoderMatchesSharedFixtures(JsonNode test) {
        var route = Route.valueOf(test.get("route").asText());
        byte[] body = test.get("body").asText().getBytes(StandardCharsets.UTF_8);
        String header = test.has("header") ? test.get("header").asText() : null;
        if (test.get("expect").asText().equals("ACCEPT")) {
            assertThat(decode(route, body, header)).isNotNull();
        } else assertThatThrownBy(() -> decode(route, body, header)).isInstanceOfSatisfying(Invalid.class,
                error -> assertThat(error.error().name()).isEqualTo(test.get("expect").asText()));
    }

    @ParameterizedTest @MethodSource("states")
    void statePrecedenceMatchesSharedFixtures(JsonNode t) {
        assertThat(SnapshotAccessRules.decide(SnapshotAccessRules.Action.valueOf(t.get("action").asText()),
                t.get("exists").asBoolean(), t.get("bindingMatches").asBoolean(), t.get("active").asBoolean(),
                t.get("expired").asBoolean(), SnapshotAccessRules.State.valueOf(t.get("status").asText())).name())
                .isEqualTo(t.get("expected").asText());
    }

    @Test void messagesStatusAndRetryPolicyMatchAllErrorFixtures() throws Exception {
        var errors = fixture("errors.json").get("errors");
        assertThat(errors.size()).isEqualTo(Error.values().length);
        for (var test : errors) {
            var error = Error.valueOf(test.get("code").asText());
            assertThat(error.status()).isEqualTo(test.get("status").asInt());
            assertThat(error.retryable()).isEqualTo(test.get("retryable").asBoolean());
            assertThat(error.message()).isEqualTo(test.get("message").asText());
            if (test.get("details").asText().equals("omitted"))
                assertThat(errorBody(error, ID).has("details")).isFalse();
        }
        assertThat(Error.RESPONSE_LIMIT_EXCEEDED.retryable()).isFalse();
        assertThat(Error.SNAPSHOT_FAILED.retryable()).isFalse();
    }

    @Test void requiredNullAndOmissionAreDifferent() {
        var details = new SourceDetails(ID, 0, 5, 3, null, Instant.EPOCH);
        assertThat(sourceErrorBody(Error.COVERAGE_GAP, ID, details).get("details").get("unavailableRange").isNull()).isTrue();
        assertThatThrownBy(() -> errorBody(Error.COVERAGE_GAP, ID)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sourceErrorBody(Error.REPLAY_UNAVAILABLE, ID, details)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sourceErrorBody(Error.SOURCE_AVAILABILITY_UNCERTAIN, ID, details))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sourceErrorBody(Error.REPLAY_UNAVAILABLE, ID,
                new SourceDetails(ID, 0, 5, 3, new UnavailableRanges.Range(3, 6), Instant.EPOCH)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void limitsAreUtf8BytesAndDoNotChangePublicJackson() throws Exception {
        String valid = "{\"streamId\":\"" + ID + "\"}";
        byte[] atLimit = (valid + " ".repeat(MAX_BYTES - valid.length())).getBytes(StandardCharsets.UTF_8);
        assertThat(decode(Route.CREATE, atLimit, ID)).isInstanceOf(Create.class);
        assertThatThrownBy(() -> decode(Route.CREATE, java.util.Arrays.copyOf(atLimit, MAX_BYTES + 1), ID))
                .isInstanceOfSatisfying(Invalid.class, e -> assertThat(e.error()).isEqualTo(Error.REQUEST_TOO_LARGE));
        var output = JSON.createObjectNode().put("synthetic", "가".repeat(6000));
        assertThatThrownBy(() -> encode(output)).isInstanceOfSatisfying(Invalid.class,
                e -> assertThat(e.error()).isEqualTo(Error.RESPONSE_LIMIT_EXCEEDED));
        assertThat(JSON.isEnabled(com.fasterxml.jackson.core.JsonParser.Feature.STRICT_DUPLICATE_DETECTION)).isFalse();
        assertThat(JSON.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)).isFalse();
    }

    @Test void rejectsMalformedUtf8Utf16AndDoesNotLogOpaqueCursor() {
        for (byte[] invalid : List.of(new byte[]{(byte) 0xc3, 0x28}, "{}".getBytes(StandardCharsets.UTF_16)))
            assertThatThrownBy(() -> decode(Route.STATUS, invalid, null)).isInstanceOf(Invalid.class);
        var request = decode(Route.FEED_PAGE, ("{\"streamId\":\"" + ID
                + "\",\"afterSequence\":\"0\",\"scanCursor\":\"synthetic.secret.cursor\"}").getBytes(StandardCharsets.UTF_8), null);
        assertThat(request.toString()).doesNotContain("synthetic.secret.cursor", ID);
    }
}
