package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Exact I2 subset of the accepted manifest. Read-only at application startup. No drop/recreate. */
public final class WithdrawalIndexes {
    public record Spec(String collection, String name, Document keys, boolean unique, Document partial) { }
    public static final List<Spec> SPECS = List.of(
        new Spec("withdrawal_stream", "ux_withdrawal_stream", new Document("environment",1).append("streamId",1),true,null),
        new Spec("withdrawal_stream", "ux_withdrawal_active_stream", new Document("environment",1),true,new Document("status","ACTIVE")),
        new Spec("withdrawal_journal", "ux_withdrawal_sequence", new Document("streamId",1).append("sequence",1),true,null),
        new Spec("withdrawal_journal", "ux_withdrawal_event", new Document("eventId",1),true,null),
        new Spec("withdrawal_journal", "ix_withdrawal_capture_expiry", new Document("capturedAt",1).append("_id",1),false,null),
        new Spec("withdrawal_deliveries", "ux_withdrawal_destination", new Document("eventId",1).append("destination",1),true,null),
        new Spec("withdrawal_deliveries", "ix_withdrawal_due", new Document("destination",1).append("status",1).append("nextAttemptAt",1).append("_id",1),false,null));
    private final MongoTemplate mongo;
    public WithdrawalIndexes(MongoTemplate mongo) { this.mongo=mongo; }
    /** Explicit operator-only DDL. Caller must diagnose existing data before invoking. */
    public void prepare(WithdrawalAdministration.Authority authority) {
        authority.require("PREPARE_INDEXES");
        for (Spec spec : SPECS) {
            Document existing = (mongo.collectionExists(spec.collection())
                    ? mongo.getCollection(spec.collection()).listIndexes().into(new java.util.ArrayList<Document>())
                    : java.util.List.<Document>of())
                    .stream().filter(i -> spec.name().equals(i.getString("name"))).findFirst().orElse(null);
            if (existing != null) validate(spec,existing);
            else {
                var options = new com.mongodb.client.model.IndexOptions().name(spec.name()).unique(spec.unique())
                        .sparse(false).collation(com.mongodb.client.model.Collation.builder().locale("simple").build());
                if (spec.partial() != null) options.partialFilterExpression(spec.partial());
                mongo.getCollection(spec.collection()).createIndex(spec.keys(),options);
            }
        }
        verify();
    }
    public void verify() {
        for (Spec spec : SPECS) {
            Document index = mongo.getCollection(spec.collection()).listIndexes().into(new java.util.ArrayList<>())
                    .stream().filter(i -> spec.name().equals(i.getString("name"))).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Withdrawal index missing: " + spec.name()));
            validate(spec, index);
        }
        // Collection default collation must also be simple (absence means simple).
        for (String collection : SPECS.stream().map(Spec::collection).distinct().toList()) {
            Document metadata = mongo.getDb().listCollections().filter(new Document("name",collection)).first();
            Document options = metadata == null ? null : metadata.get("options", Document.class);
            if (options != null && !simple(options.get("collation", Document.class)))
                throw new IllegalStateException("Withdrawal collection collation mismatch.");
        }
    }
    public static void validate(Spec spec, Document actual) {
        Document keys = actual.get("key", Document.class);
        if (keys == null || !List.copyOf(keys.entrySet()).equals(List.copyOf(spec.keys().entrySet()))
                || actual.getBoolean("unique",false) != spec.unique() || actual.getBoolean("sparse",false)
                || actual.containsKey("expireAfterSeconds")
                || !java.util.Objects.equals(spec.partial(), actual.get("partialFilterExpression"))
                || !simple(actual.get("collation",Document.class)))
            throw new IllegalStateException("Withdrawal index mismatch: " + spec.name());
    }
    private static boolean simple(Document collation) {
        return collation == null || collation.equals(new Document("locale","simple"));
    }
}
