package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** Numeric observations only. Alert routing is a deployment responsibility, not a webhook containing user facts. */
public final class WithdrawalMonitor {
    private final MongoTemplate mongo;
    private final Clock clock;
    private final MeterRegistry metrics;
    private final AtomicLong overdue=new AtomicLong(), expiry=new AtomicLong(), review=new AtomicLong();
    public WithdrawalMonitor(MongoTemplate mongo, Clock clock, MeterRegistry metrics) {
        this.mongo=mongo; this.clock=clock; this.metrics=metrics;
        Gauge.builder("identity.withdrawal.billing.overdue_24h",overdue,AtomicLong::get).register(metrics);
        Gauge.builder("identity.withdrawal.source.expiring_7d",expiry,AtomicLong::get).register(metrics);
        Gauge.builder("identity.withdrawal.billing.review_due",review,AtomicLong::get).register(metrics);
    }
    @Scheduled(fixedDelay=60000)
    public void observe() {
        try {
            var now=clock.instant();
            var match=new Document("$match",new Document("capturedAt",new Document("$lte",java.util.Date.from(now.minus(Duration.ofHours(24))))));
            var lookup=new Document("$lookup",new Document("from","withdrawal_deliveries")
                    .append("localField","eventId").append("foreignField","eventId").append("as","delivery"));
            var pending=new Document("$match",new Document("delivery",new Document("$elemMatch",
                    new Document("destination",BILLING).append("status",new Document("$ne","DELIVERED")))));
            var aggregation=Aggregation.newAggregation(context -> match,context -> lookup,context -> pending,
                    context -> new Document("$count","count"));
            var result=mongo.aggregate(aggregation,"withdrawal_journal",Document.class).getUniqueMappedResult();
            overdue.set(result==null?0:((Number)result.get("count")).longValue());
            expiry.set(mongo.count(Query.query(Criteria.where("capturedAt").lte(now.minus(Duration.ofDays(113)))),Journal.class));
            review.set(mongo.count(Query.query(Criteria.where("reviewDueAt").lte(now)
                    .and("status").nin(State.DELIVERED,State.PENDING,State.IN_FLIGHT)),Delivery.class));
        } catch (RuntimeException failure) {
            metrics.counter("identity.withdrawal.monitor_failure").increment(); // retain prior observations, never false zero
        }
    }
}
