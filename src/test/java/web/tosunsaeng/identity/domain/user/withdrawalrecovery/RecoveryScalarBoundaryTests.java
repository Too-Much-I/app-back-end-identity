package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocolTests.*;

/** Shared scalar-end-1 cases run through the actual networknt validator, not a surrogate regex. */
class RecoveryScalarBoundaryTests {
    record Boundary(String name, String definition, String value) { }

    static Stream<Boundary> boundaries() throws Exception {
        var fixture = fixture("scalar-cases.json");
        var result = new ArrayList<Boundary>();
        for (var sample : fixture.get("samples")) for (var forbidden : fixture.get("forbidden"))
            for (var position : fixture.get("positions")) {
                String s = sample.get("value").asText(), insertion = forbidden.get("value").asText();
                String value = switch (position.asText()) {
                    case "prefix" -> insertion + s;
                    case "infix" -> s.substring(0, 1) + insertion + s.substring(1);
                    case "suffix" -> s + insertion;
                    default -> throw new IllegalArgumentException("Unknown fixture position.");
                };
                result.add(new Boundary(sample.get("definition").asText() + "/" + forbidden.get("name").asText()
                        + "/" + position.asText(), sample.get("definition").asText(), value));
            }
        return result.stream();
    }
    static Stream<Arguments> schemaCases() throws Exception {
        return boundaries().map(b -> Arguments.of(b.name(), b.definition(), b.value()));
    }
    static Stream<Arguments> decoderCases() throws Exception {
        return boundaries().filter(b -> Set.of("D0", "NewId", "Cursor").contains(b.definition()))
                .map(b -> Arguments.of(b.name(), b.definition(), b.value()));
    }
    private JsonSchema schema(String definition) throws Exception {
        ObjectNode root = (ObjectNode) fixture("wire.schema.json");
        root.put("$ref", "#/$defs/" + definition);
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root);
    }

    @ParameterizedTest(name = "{0}") @MethodSource("schemaCases")
    void schemaRejectsAllControlPlacements(String name, String definition, String value) throws Exception {
        assertThat(schema(definition).validate(JSON.getNodeFactory().textNode(value))).as(name).isNotEmpty();
    }

    @ParameterizedTest(name = "{0}") @MethodSource("decoderCases")
    void existingStrictRequestDecoderStillRejectsThem(String name, String definition, String value) throws Exception {
        var body = JSON.createObjectNode().put("streamId", ID).put("afterSequence", "1").putNull("scanCursor");
        body.put(switch (definition) {
            case "D0" -> "afterSequence"; case "NewId" -> "streamId"; default -> "scanCursor";
        }, value);
        assertThatThrownBy(() -> RecoveryProtocol.decode(RecoveryProtocol.Route.FEED_PAGE,
                body.toString().getBytes(StandardCharsets.UTF_8), null))
                .as(name).isInstanceOfSatisfying(RecoveryProtocol.Invalid.class,
                        e -> assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.INVALID_REQUEST));
    }

    @Test void allPatternsAreCoveredAndValidSamplesStayValid() throws Exception {
        var wire = fixture("wire.schema.json");
        Set<String> actual = new HashSet<>(); collectPatterns(wire.get("$defs"), "", actual);
        Set<String> covered = new HashSet<>();
        for (var sample : fixture("scalar-cases.json").get("samples")) {
            String def = sample.get("definition").asText(); covered.add(def);
            assertThat(schema(def).validate(sample.get("value"))).as(def).isEmpty();
        }
        assertThat(actual).hasSize(8).isEqualTo(covered);
        assertThat(boundaries().count()).isEqualTo(264);
        String printable = java.util.stream.IntStream.rangeClosed(0x21, 0x7e)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        assertThat(schema("Cursor").validate(JSON.getNodeFactory().textNode(printable))).isEmpty();
    }

    private void collectPatterns(JsonNode node, String path, Set<String> paths) {
        if (!node.isObject()) return;
        if (node.has("pattern")) {
            assertThat(node.get("pattern").asText()).endsWith("(?![\\s\\S])").doesNotContain("\\z");
            paths.add(path);
        }
        node.fields().forEachRemaining(e -> collectPatterns(e.getValue(), path.isEmpty() ? e.getKey() : path + "/" + e.getKey(), paths));
    }
}
