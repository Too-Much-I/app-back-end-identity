package web.tosunsaeng.identity.domain.support;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;

/** Internal operator primitive, deliberately not a bean or HTTP endpoint. Approval is external. */
public final class SupportOperations {
    public static final String AUDIT = "support_inquiry_audits";
    private final MongoTemplate mongo;
    private final SupportStore store;
    public SupportOperations(MongoTemplate mongo, SupportStore store) { this.mongo = mongo; this.store = store; }

    public void replayFailed(String id, int expectedAttempts, String approvalReference, Instant now) {
        validate(id, approvalReference);
        store.transaction(() -> {
            var content = store.content(id);
            if (content == null || !content.expiresAt().isAfter(now)) throw new IllegalStateException("INQUIRY_UNAVAILABLE");
            var query = Query.query(Criteria.where("_id").is(id).and("status").is("FAILED")
                    .and("attempts").is(expectedAttempts).and("manualReplayCount").exists(false));
            var result = mongo.updateFirst(query, new Update().set("status", "RETRY_WAIT").set("attempts", 0)
                    .set("manualReplayCount", 1).set("nextAttemptAt", Date.from(now))
                    .set("updatedAt", Date.from(now)), MongoSupportStore.DELIVERIES);
            if (result.getModifiedCount() != 1) throw new IllegalStateException("DELIVERY_STATE_CONFLICT");
            audit(id, "REPLAY_FAILED", approvalReference, now);
            return null;
        });
    }

    /** Stop every worker and wait for in-flight HTTP requests before privacy deletion. */
    public void deleteInquiry(String id, String approvalReference, Instant now) {
        validate(id, approvalReference);
        store.transaction(() -> {
            var delivery = mongo.findById(id, Document.class, MongoSupportStore.DELIVERIES);
            if (delivery != null && "SENDING".equals(delivery.getString("status")))
                throw new IllegalStateException("DELIVERY_IN_FLIGHT");
            mongo.remove(Query.query(Criteria.where("_id").is(id)), MongoSupportStore.DELIVERIES);
            mongo.remove(Query.query(Criteria.where("inquiryId").is(id)), MongoSupportStore.REQUESTS);
            mongo.remove(Query.query(Criteria.where("_id").is(id)), MongoSupportStore.INQUIRIES);
            audit(id, "DELETE_INQUIRY", approvalReference, now);
            return null;
        });
    }
    private void audit(String id, String action, String reference, Instant now) {
        mongo.insert(new Document("_id", UUID.randomUUID().toString()).append("inquiryId", id)
                .append("action", action).append("approvalReference", reference).append("createdAt", Date.from(now))
                .append("expiresAt", Date.from(now.plusSeconds(90L * 86400))), AUDIT);
    }
    private static void validate(String id, String reference) {
        if (id == null || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
                || reference == null || !reference.matches("[A-Z][A-Z0-9]{1,15}-[0-9]{1,12}"))
            throw new IllegalArgumentException("INVALID_OPERATION_REFERENCE");
    }
}
