package web.tosunsaeng.identity.domain.support;

import java.time.Instant;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.TransactionTemplate;

public final class MongoSupportStore implements SupportStore {
    public static final String INQUIRIES = "support_inquiries", REQUESTS = "support_inquiry_requests",
            DELIVERIES = "support_inquiry_deliveries", QUOTAS = "support_inquiry_quotas";
    private final MongoTemplate mongo;
    private final TransactionTemplate tx;
    public MongoSupportStore(MongoTemplate mongo, TransactionTemplate tx) { this.mongo = mongo; this.tx = tx; }
    public void ensureIndexes() {
        for (String collection : new String[]{INQUIRIES, REQUESTS, DELIVERIES, QUOTAS, SupportOperations.AUDIT}) {
            mongo.indexOps(collection).ensureIndex(new Index().on("expiresAt", Sort.Direction.ASC).expire(Duration.ZERO));
        }
        mongo.indexOps(DELIVERIES).ensureIndex(new Index().on("status", Sort.Direction.ASC).on("nextAttemptAt", Sort.Direction.ASC));
        mongo.indexOps(DELIVERIES).ensureIndex(new Index().on("status", Sort.Direction.ASC).on("leaseUntil", Sort.Direction.ASC));
    }
    public <T> T transaction(Supplier<T> work) { return tx.execute(status -> work.get()); }
    public Receipt receipt(String key) {
        Document d = mongo.findById(key, Document.class, REQUESTS);
        return d == null ? null : new Receipt(d.getString("inquiryId"), d.getString("digest"), d.getDate("expiresAt").toInstant());
    }
    public void removeExpiredReceipt(String key, Instant now) {
        mongo.remove(Query.query(Criteria.where("_id").is(key).and("expiresAt").lte(Date.from(now))), REQUESTS);
    }
    public void quota(String key, int limit, Instant expires) {
        Document d = mongo.findAndModify(Query.query(Criteria.where("_id").is(key)),
                new Update().inc("count", 1).setOnInsert("expiresAt", Date.from(expires)),
                FindAndModifyOptions.options().upsert(true).returnNew(true), Document.class, QUOTAS);
        if (d == null || ((Number)d.get("count")).longValue() > limit) throw SupportError.SUPPORT_INQUIRY_RATE_LIMITED.exception();
    }
    public void insert(String id, String key, String digest, String userId, String actorType, SupportRequest r, Instant now) {
        Date expires = Date.from(now.plus(Duration.ofDays(90)));
        Document context = new Document();
        if (r.context() != null) {
            var c = r.context(); context.append("screen", c.screen()).append("errorCode", c.errorCode())
                    .append("requestId", c.requestId()).append("appVersion", c.appVersion())
                    .append("platform", c.platform() == null ? null : c.platform().name());
        }
        mongo.insert(new Document("_id", key).append("inquiryId", id).append("digest", digest)
                .append("expiresAt", Date.from(now.plus(Duration.ofDays(7)))), REQUESTS);
        mongo.insert(new Document("_id", id).append("userId", userId).append("actorType", actorType)
                .append("category", r.category().name()).append("message", r.message()).append("replyEmail", r.replyEmail())
                .append("context", context).append("status", "RECEIVED").append("createdAt", Date.from(now)).append("expiresAt", expires), INQUIRIES);
        // One delivery per inquiry; the built-in _id unique index prevents duplicate outbox rows.
        mongo.insert(new Document("_id", id).append("destination", "SLACK").append("status", "PENDING")
                .append("attempts", 0).append("nextAttemptAt", Date.from(now)).append("createdAt", Date.from(now))
                .append("expiresAt", expires), DELIVERIES);
    }
    public Delivery claim(Instant now) {
        Criteria due = new Criteria().orOperator(
                Criteria.where("status").in("PENDING", "RETRY_WAIT").and("nextAttemptAt").lte(Date.from(now)),
                Criteria.where("status").is("SENDING").and("leaseUntil").lte(Date.from(now)));
        String lease = UUID.randomUUID().toString();
        Document d = mongo.findAndModify(Query.query(due).with(Sort.by("createdAt")),
                new Update().set("status", "SENDING").set("leaseToken", lease).set("leaseUntil", Date.from(now.plusSeconds(60))).inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true), Document.class, DELIVERIES);
        return d == null ? null : new Delivery(d.getString("_id"), lease, ((Number)d.get("attempts")).intValue(), d.getDate("expiresAt").toInstant());
    }
    public Content content(String id) {
        Document d = mongo.findById(id, Document.class, INQUIRIES);
        return d == null ? null : new Content(d.getString("message"), d.getDate("expiresAt").toInstant());
    }
    public void finish(Delivery delivery, String status, Instant next, String error, Instant now) {
        mongo.updateFirst(Query.query(Criteria.where("_id").is(delivery.id()).and("status").is("SENDING")
                        .and("leaseToken").is(delivery.lease())),
                new Update().set("status", status).set("nextAttemptAt", Date.from(next)).set("lastErrorCategory", error)
                        .set("updatedAt", Date.from(now)).unset("leaseToken").unset("leaseUntil"), DELIVERIES);
    }
}
