package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class RecoveryIndexesTests {
    private List<Document> indexes(String collection) {
        var result = new ArrayList<Document>();
        result.add(new Document("name", "_id_").append("key", new Document("_id", 1)));
        RecoveryIndexes.SPECS.stream().filter(s -> s.collection().equals(collection)).forEach(s -> {
            var index = new Document("name", s.name()).append("key", s.keys()).append("unique", s.unique());
            if (s.partial() != null) index.append("partialFilterExpression", s.partial());
            result.add(index);
        });
        return result;
    }
    private void validate(List<Document> indexes) {
        RecoveryIndexes.validateCollection(RecoveryIndexes.SPECS.stream().filter(s -> s.collection().equals("withdrawal_stream")).toList(), indexes);
    }
    @Test void acceptsSixteenExpectedIndexesAndNormalId() {
        assertThat(RecoveryIndexes.SPECS).hasSize(16);
        for (var name : RecoveryIndexes.SPECS.stream().map(s -> s.collection()).distinct().toList())
            RecoveryIndexes.validateCollection(RecoveryIndexes.SPECS.stream().filter(s -> s.collection().equals(name)).toList(), indexes(name));
    }
    @Test void matchesAcceptedManifestIncludingKeyOrder() throws Exception {
        try (var input = getClass().getResourceAsStream("/contracts/payment-lifecycle/recovery-r1/identity-indexes.json")) {
            var entries = new com.fasterxml.jackson.databind.ObjectMapper().readTree(input).get("indexes");
            assertThat(entries.size()).isEqualTo(RecoveryIndexes.SPECS.size());
            for (var entry : entries) {
                var spec = RecoveryIndexes.SPECS.stream().filter(s -> s.name().equals(entry.get("name").asText())).findFirst().orElseThrow();
                assertThat(spec.collection()).isEqualTo(entry.get("collection").asText());
                assertThat(new ArrayList<>(spec.keys().entrySet())).isEqualTo(new ArrayList<>(Document.parse(entry.get("keys").toString()).entrySet()));
                assertThat(spec.unique()).isEqualTo(entry.get("unique").asBoolean());
                assertThat(spec.partial()).isEqualTo(entry.has("partialFilterExpression") ? Document.parse(entry.get("partialFilterExpression").toString()) : null);
            }
        }
    }
    @Test void rejectsTtlHiddenMissingAndUnexpectedUnique() {
        for (String option : List.of("expireAfterSeconds", "hidden")) {
            var actual = indexes("withdrawal_stream"); actual.get(1).put(option, option.equals("hidden") ? true : 60);
            assertThatThrownBy(() -> validate(actual)).isInstanceOf(IllegalStateException.class);
        }
        var missing = indexes("withdrawal_stream"); missing.removeFirst();
        assertThatThrownBy(() -> validate(missing)).isInstanceOf(IllegalStateException.class);
        var extra = indexes("withdrawal_stream"); extra.add(new Document("name", "legacy").append("unique", true));
        assertThatThrownBy(() -> validate(extra)).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsIdCollationAndReorderedCompoundKeys() {
        var actual = indexes("withdrawal_stream"); actual.getFirst().put("collation", new Document("locale", "en"));
        assertThatThrownBy(() -> validate(actual)).isInstanceOf(IllegalStateException.class);
        var reordered = indexes("withdrawal_stream"); reordered.get(1).put("key", new Document("streamId", 1).append("environment", 1));
        assertThatThrownBy(() -> validate(reordered)).isInstanceOf(IllegalStateException.class);
    }
    static Document stream() {
        return new Document("_id", "source").append("environment", "test").append("streamId", "00000000-0000-4000-8000-000000000001")
                .append("status", "ACTIVE").append("lastSequence", 500L).append("replayFloor", 0L)
                .append("availabilityVersion", 1L).append("normalizationAfterSequence", 0L)
                .append("normalizationCarryFromSequence", null).append("closedAt", null);
    }
    @Test void requiresExplicitLongMigrationFieldsAndClosedTimestamp() {
        RecoveryIndexes.validateStream(stream());
        for (String field : List.of("availabilityVersion", "normalizationAfterSequence", "normalizationCarryFromSequence", "closedAt")) {
            var source = stream(); source.remove(field);
            assertThatThrownBy(() -> RecoveryIndexes.validateStream(source)).isInstanceOf(RuntimeException.class);
        }
        var source = stream(); source.put("lastSequence", 500);
        assertThatThrownBy(() -> RecoveryIndexes.validateStream(source)).isInstanceOf(RuntimeException.class);
        source.put("lastSequence", 500L); source.put("status", "CLOSED_RESTORE");
        assertThatThrownBy(() -> RecoveryIndexes.validateStream(source)).isInstanceOf(RuntimeException.class);
        source.put("closedAt", new Date()); RecoveryIndexes.validateStream(source);
    }
}
