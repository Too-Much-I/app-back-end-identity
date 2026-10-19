package web.tosunsaeng.identity.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** TMI-199 test-only exact-spec acceptance; no production parser or index initializer. */
class PaymentLifecycleExactSpecTests {
    static Stream<JsonNode> boundaries() throws Exception {
        List<JsonNode> cases = new ArrayList<>();
        PaymentLifecycleFixtureTests.fixture("numeric-boundaries").get("cases").forEach(cases::add);
        assertThat(cases).hasSize(39);
        return cases.stream();
    }

    @ParameterizedTest
    @MethodSource("boundaries")
    void independentlyParsesBoundedDecimalWithoutCoercion(JsonNode test) {
        boolean accepted;
        try {
            var value = test.get("value");
            if (!value.isTextual() || !value.textValue().matches("0|[1-9][0-9]*"))
                throw new IllegalArgumentException();
            long parsed = Long.parseLong(value.textValue());
            accepted = switch (test.get("kind").asText()) {
                case "SEQUENCE" -> parsed >= 1;
                case "COUNTER" -> parsed >= 0;
                case "TIMESTAMP_PART" -> parsed >= 0 && parsed <= 0xffff_ffffL;
                default -> throw new AssertionError("Unknown boundary kind");
            };
        } catch (IllegalArgumentException invalid) { accepted = false; }
        assertThat(accepted).as(test.toString()).isEqualTo(test.get("accepted").booleanValue());
    }

    @Test
    void incrementMustRejectOverflowRatherThanWrap() {
        assertThat(Math.incrementExact(Long.MAX_VALUE - 1)).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(() -> Math.incrementExact(Long.MAX_VALUE)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void exactSharedInputsArePinned() throws Exception {
        assertThat(PaymentLifecycleFixtureTests.sha256(PaymentLifecycleFixtureTests.bytes("numeric-boundaries")))
                .isEqualTo("f72a4e61d40825d540d60aa135839510edf3981658fd65b7fe337873a1bc24eb");
        assertThat(PaymentLifecycleFixtureTests.sha256(PaymentLifecycleFixtureTests.bytes("identity-indexes")))
                .isEqualTo("74d69edbfa244c619c9cb778da46abb8c851568dc1f673921e8443a0f557e55f");
    }

    @Test
    void identityManifestHasThirteenOrderedAdditiveIndexesWithoutTtl() throws Exception {
        var indexes = PaymentLifecycleFixtureTests.fixture("identity-indexes").get("indexes");
        assertThat(indexes.size()).isEqualTo(13);
        Set<String> names = new HashSet<>();
        int partialCount = 0;
        for (var index : indexes) {
            assertThat(names.add(index.get("collection").asText() + "/" + index.get("name").asText())).isTrue();
            assertThat(index.has("expireAfterSeconds")).isFalse();
            assertThat(index.path("sparse").asBoolean(false)).isFalse();
            assertThat(index.get("unique").isBoolean()).isTrue();
            List<String> keys = new ArrayList<>();
            index.get("keys").fieldNames().forEachRemaining(keys::add);
            index.get("keys").forEach(direction -> assertThat(direction.intValue()).isEqualTo(1));
            if (index.get("name").asText().equals("ux_withdrawal_sequence")) {
                assertThat(keys).containsExactly("streamId", "sequence");
                assertThat(index.get("unique").booleanValue()).isTrue();
            }
            if (index.has("partialFilterExpression")) {
                partialCount++;
                assertThat(index.get("name").asText()).isEqualTo("ux_withdrawal_active_stream");
                assertThat(keys).containsExactly("environment");
                assertThat(index.get("partialFilterExpression").size()).isEqualTo(1);
                assertThat(index.get("partialFilterExpression").get("status").asText()).isEqualTo("ACTIVE");
            }
        }
        assertThat(partialCount).isEqualTo(1);
    }
}
