package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** Every worker write is fenced by lease identity, version and resume generation. */
public final class WithdrawalDeliveryStore {
    private final MongoTemplate mongo;
    public WithdrawalDeliveryStore(MongoTemplate mongo) { this.mongo = mongo; }
    public Delivery claim(String owner, Instant now) {
        Criteria due = new Criteria().orOperator(
                Criteria.where("status").is(State.PENDING).and("nextAttemptAt").lte(now),
                Criteria.where("status").is(State.IN_FLIGHT).and("leaseUntil").lte(now));
        Query query = Query.query(new Criteria().andOperator(due, Criteria.where("destination").is(BILLING)
                .and("notBefore").lte(now).and("leaseVersion").lt(Long.MAX_VALUE)))
                .with(Sort.by("nextAttemptAt", "_id"));
        return mongo.findAndModify(query, new Update().set("status", State.IN_FLIGHT)
                .set("leaseOwner", owner).set("leaseUntil", now.plusSeconds(60)).inc("leaseVersion", 1L),
                FindAndModifyOptions.options().returnNew(true), Delivery.class);
    }
    public Journal journal(Delivery d) { return mongo.findById(d.eventId(), Journal.class); }
    public boolean activeStream(Journal j, String environment) {
        return mongo.exists(Query.query(Criteria.where("_id").is(j.streamId())
                .and("environment").is(environment).and("status").is("ACTIVE")), Stream.class);
    }
    public Delivery dispatch(Delivery d, Instant now) {
        Query query = lease(d, now);
        query.addCriteria(Criteria.where("generationAttemptCount").is(d.generationAttemptCount()).lt(20L)
                .and("lifetimeAttemptCount").is(d.lifetimeAttemptCount()).lt(Long.MAX_VALUE));
        return mongo.findAndModify(query, new Update().inc("generationAttemptCount", 1L)
                .inc("lifetimeAttemptCount", 1L).set("dispatchMarkedAt", now),
                FindAndModifyOptions.options().returnNew(true), Delivery.class);
    }
    public boolean finish(Delivery d, BillingWithdrawalRetry.Decision decision, Instant now) {
        Update change = new Update().set("status", decision.state()).set("lastFailureCode", decision.reason())
                .set("leaseOwner", null).set("leaseUntil", null);
        if (decision.state() == State.DELIVERED) change.set("deliveredAt", now);
        else {
            change.set("nextAttemptAt", decision.nextAttemptAt()).set("notBefore", decision.nextAttemptAt());
            if (decision.state() != State.PENDING)
                change.set("reviewDueAt", d.reviewDueAt() == null ? now.plus(Duration.ofDays(7)) : d.reviewDueAt());
        }
        return mongo.updateFirst(lease(d, now), change, Delivery.class).getModifiedCount() == 1;
    }
    public Delivery find(String eventId) { return mongo.findById(uuid(eventId) + ":BILLING", Delivery.class); }
    public boolean resume(Delivery d, String operationId, String reason, String configVersion, Instant now) {
        Query query = Query.query(Criteria.where("_id").is(d.id()).and("resumeGeneration").is(d.resumeGeneration())
                .and("leaseVersion").is(d.leaseVersion()).and("status").in(State.DEAD_LETTER,
                        State.BLOCKED_AUTH, State.BLOCKED_RETRY_AFTER, State.BLOCKED_CLOCK, State.BLOCKED_HEADER)
                .and("notBefore").lte(now));
        return mongo.updateFirst(query, new Update().set("status", State.PENDING)
                .inc("resumeGeneration", 1L).inc("leaseVersion", 1L).set("generationAttemptCount", 0L)
                .set("resumeOperationId", operationId).set("nextAttemptAt", now)
                .set("resumeReason", reason).set("resumeConfigVersion", configVersion)
                .set("leaseOwner", null).set("leaseUntil", null), Delivery.class).getModifiedCount() == 1;
    }
    public List<Journal> expiryCandidates(Instant now, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Bounded batch required.");
        return mongo.find(Query.query(Criteria.where("capturedAt").lte(now.minus(RETENTION)))
                .with(Sort.by("capturedAt", "_id")).limit(limit), Journal.class);
    }
    private static Query lease(Delivery d, Instant now) {
        return Query.query(Criteria.where("_id").is(d.id()).and("destination").is(BILLING)
                .and("status").is(State.IN_FLIGHT).and("leaseOwner").is(d.leaseOwner())
                .and("leaseVersion").is(d.leaseVersion()).and("resumeGeneration").is(d.resumeGeneration())
                .and("leaseUntil").gt(now));
    }
}
