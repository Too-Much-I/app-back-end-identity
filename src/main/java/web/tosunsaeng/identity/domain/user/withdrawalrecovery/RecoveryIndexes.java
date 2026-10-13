package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalIndexes;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalIndexes.Spec;

/** Read-only startup gate. No automatic collection/index creation or migration. */
public final class RecoveryIndexes {
    public static final List<Spec> SPECS;
    static {
        var specs = new ArrayList<>(WithdrawalIndexes.SPECS);
        specs.add(new Spec("withdrawal_snapshot_runs", "ux_withdrawal_snapshot_operation_r1", new Document("environment", 1).append("consumer", 1).append("requestOperationId", 1), true, null));
        specs.add(new Spec("withdrawal_snapshot_runs", "ux_withdrawal_snapshot", new Document("snapshotId", 1), true, null));
        specs.add(new Spec("withdrawal_snapshot_rows", "ux_withdrawal_snapshot_user", new Document("snapshotId", 1).append("userId", 1), true, null));
        specs.add(new Spec("withdrawal_unavailable_ranges", "ux_withdrawal_unavailable_from", new Document("streamId", 1).append("fromSequence", 1), true, null));
        specs.add(new Spec("withdrawal_unavailable_ranges", "ix_withdrawal_unavailable_to", new Document("streamId", 1).append("toSequence", 1), false, null));
        specs.add(new Spec("withdrawal_snapshot_runs", "ix_withdrawal_snapshot_privacy_expiry", new Document("privacyExpiresAt", 1).append("_id", 1), false, null));
        specs.add(new Spec("withdrawal_snapshot_runs", "ix_withdrawal_snapshot_operation_expiry", new Document("operationExpiresAt", 1).append("_id", 1), false, null));
        specs.add(new Spec("withdrawal_snapshot_runs", "ix_withdrawal_snapshot_build_lease", new Document("status", 1).append("leaseUntil", 1).append("_id", 1), false, null));
        specs.add(new Spec("withdrawal_stream", "ix_withdrawal_retired_stream", new Document("status", 1).append("closedAt", 1).append("_id", 1), false, null));
        SPECS = List.copyOf(specs);
    }
    private final MongoTemplate mongo;
    public RecoveryIndexes(MongoTemplate mongo) { this.mongo = Objects.requireNonNull(mongo); }

    public void verify() {
        for (String name : SPECS.stream().map(Spec::collection).distinct().toList()) {
            var metadata = mongo.getDb().listCollections().filter(new Document("name", name)).first();
            if (metadata == null) throw invalid();
            var options = metadata.get("options", Document.class);
            if (options != null && options.containsKey("collation") && !new Document("locale", "simple").equals(options.get("collation"))) throw invalid();
            var actual = mongo.getCollection(name).listIndexes().into(new ArrayList<Document>());
            validateCollection(SPECS.stream().filter(s -> name.equals(s.collection())).toList(), actual);
        }
    }

    public static void validateCollection(List<Spec> expected, List<Document> actual) {
        var names = new HashSet<String>();
        for (var index : actual) {
            String name = index.getString("name");
            if (name == null || !names.add(name) || index.containsKey("expireAfterSeconds")) throw invalid();
            if (!"_id_".equals(name) && index.getBoolean("unique", false) && expected.stream().noneMatch(s -> name.equals(s.name()))) throw invalid();
        }
        var id = actual.stream().filter(i -> "_id_".equals(i.getString("name"))).findFirst().orElseThrow(RecoveryIndexes::invalid);
        if (!new Document("_id", 1).equals(id.get("key")) || id.getBoolean("sparse", false)
                || id.getBoolean("hidden", false) || id.containsKey("partialFilterExpression")
                || id.containsKey("collation") && !new Document("locale", "simple").equals(id.get("collation"))
                || id.containsKey("unique") && !Boolean.TRUE.equals(id.get("unique"))) throw invalid();
        for (var spec : expected) {
            Document index = actual.stream().filter(i -> spec.name().equals(i.getString("name"))).findFirst().orElseThrow(RecoveryIndexes::invalid);
            WithdrawalIndexes.validate(spec, index);
            if (index.getBoolean("hidden", false)) throw invalid();
        }
    }

    public void verifyMigration(String environment) {
        int active = 0;
        try (var cursor = mongo.getCollection("withdrawal_stream").find(new Document("environment", environment)).batchSize(100).iterator()) {
            while (cursor.hasNext()) {
                Document stream = cursor.next(); validateStream(stream);
                if ("ACTIVE".equals(stream.getString("status"))) active++;
            }
        }
        if (active != 1) throw invalid();
    }
    public static void validateStream(Document source) {
        RecoveryProtocol.uuid4(source.getString("streamId"));
        long head = MongoRecoveryReader.nonnegativeLong(source, "lastSequence");
        long floor = MongoRecoveryReader.nonnegativeLong(source, "replayFloor");
        long version = MongoRecoveryReader.nonnegativeLong(source, "availabilityVersion");
        long after = MongoRecoveryReader.nonnegativeLong(source, "normalizationAfterSequence");
        if (floor > head || after > head || version == Long.MAX_VALUE || !source.containsKey("normalizationCarryFromSequence") || !source.containsKey("closedAt")) throw invalid();
        Object carry = source.get("normalizationCarryFromSequence");
        if (carry != null && (!(carry instanceof Long value) || value < 1 || value > after)) throw invalid();
        if ("ACTIVE".equals(source.getString("status"))) { if (source.get("closedAt") != null) throw invalid(); }
        else if (!"CLOSED_RESTORE".equals(source.getString("status")) || !(source.get("closedAt") instanceof Date)) throw invalid();
    }
    private static IllegalStateException invalid() { return new IllegalStateException("Recovery index or source migration is not ready."); }
}
