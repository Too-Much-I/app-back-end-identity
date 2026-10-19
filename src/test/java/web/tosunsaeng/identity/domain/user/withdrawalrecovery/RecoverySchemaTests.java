package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocolTests.*;

class RecoverySchemaTests {
    @Test void packetHashesPinTheSharedInputs() throws Exception {
        var entries = fixture("hashes.json").get("files").fields();
        int count = 0;
        while (entries.hasNext()) {
            var entry = entries.next(); count++;
            try (var in = getClass().getResourceAsStream(ROOT + entry.getKey())) {
                assertThat(in).isNotNull();
                String hash = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(in.readAllBytes()));
                assertThat(hash).as(entry.getKey()).isEqualTo(entry.getValue().asText());
            }
        }
        assertThat(count).isEqualTo(8);
    }

    static Stream<JsonNode> responses() throws Exception {
        List<JsonNode> result = new ArrayList<>(); fixture("response-cases.json").get("cases").forEach(result::add);
        return result.stream();
    }
    private com.networknt.schema.JsonSchema schema(String definition) throws Exception {
        ObjectNode root = (ObjectNode) fixture("wire.schema.json");
        root.put("$ref", "#/$defs/" + definition);
        return JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root);
    }
    @ParameterizedTest @MethodSource("responses")
    void responseAndNumericFixturesMatchDraft202012Schema(JsonNode test) throws Exception {
        var errors = schema(test.get("definition").asText()).validate(test.get("value"));
        assertThat(errors.isEmpty()).as(test.get("name").asText()).isEqualTo(test.get("valid").asBoolean());
    }
    @Test void everyGeneratedErrorMatchesSchema() throws Exception {
        var schema = schema("Error");
        for (var e : RecoveryProtocol.Error.values()) {
            var body = switch (e) {
                case COVERAGE_GAP -> RecoveryProtocol.sourceErrorBody(e, ID,
                        new RecoveryProtocol.SourceDetails(ID, 0, 10, 1, null, java.time.Instant.EPOCH));
                case REPLAY_UNAVAILABLE -> RecoveryProtocol.sourceErrorBody(e, ID,
                        new RecoveryProtocol.SourceDetails(ID, 0, 10, 1, new UnavailableRanges.Range(1, 2), java.time.Instant.EPOCH));
                default -> RecoveryProtocol.errorBody(e, ID);
            };
            assertThat(schema.validate(body)).as(e.name()).isEmpty();
        }
    }
    @Test void validRequestFixturesMatchRouteSpecificSchema() throws Exception {
        for (var test : fixture("request-cases.json").get("cases")) {
            if (!test.get("expect").asText().equals("ACCEPT")) continue;
            String definition = switch (test.get("route").asText()) {
                case "CREATE" -> "CreateRequest"; case "STATUS" -> "StatusRequest";
                case "SNAPSHOT_PAGE" -> "SnapshotPageRequest"; default -> "FeedPageRequest";
            };
            assertThat(schema(definition).validate(JSON.readTree(test.get("body").asText()))).isEmpty();
        }
    }
    @Test void indexManifestPreservesI2AndMakesNewScopeExplicit() throws Exception {
        var manifest = fixture("identity-indexes.json");
        var indexes = manifest.get("indexes");
        assertThat(indexes.size()).isEqualTo(16).isEqualTo(manifest.get("declaredIndexCount").asInt());
        var names = new java.util.HashSet<String>();
        for (var i : indexes) {
            assertThat(names.add(i.get("collection").asText() + "/" + i.get("name").asText())).isTrue();
            assertThat(i.get("sparse").asBoolean()).isFalse();
            assertThat(i.get("collation").get("locale").asText()).isEqualTo("simple");
            assertThat(i.has("expireAfterSeconds")).isFalse();
            assertThat(i.get("keys").has("consumerRecoveryGeneration")).isFalse();
        }
        var specs = web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalIndexes.SPECS;
        for (int n = 0; n < specs.size(); n++) {
            var s = specs.get(n); var f = indexes.get(n);
            assertThat(f.get("collection").asText()).isEqualTo(s.collection());
            assertThat(f.get("name").asText()).isEqualTo(s.name());
            assertThat(f.get("unique").asBoolean()).isEqualTo(s.unique());
            assertThat(JSON.readTree(s.keys().toJson()).toString()).isEqualTo(f.get("keys").toString());
            if (s.partial() == null) assertThat(f.has("partialFilterExpression")).isFalse();
            else assertThat(f.get("partialFilterExpression")).isEqualTo(JSON.readTree(s.partial().toJson()));
        }
        assertThat(indexes.get(7).get("name").asText()).isEqualTo("ux_withdrawal_snapshot_operation_r1");
        assertThat(indexes.get(7).get("keys").toString())
                .isEqualTo("{\"environment\":1,\"consumer\":1,\"requestOperationId\":1}");
        assertThat(manifest.get("productionInitializer").asBoolean()).isFalse();
    }
}
