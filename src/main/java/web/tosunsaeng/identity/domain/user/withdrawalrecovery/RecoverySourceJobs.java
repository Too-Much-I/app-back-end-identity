package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.Objects;
import io.micrometer.core.instrument.MeterRegistry;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalSourceCleanup;

/** Fair bounded traversal. The local cursor is a scheduling hint, never a recovery checkpoint. */
public final class RecoverySourceJobs {
    private final WithdrawalSourceCleanup cleanup;
    private final MongoSourceSweep sweep;
    private final MongoRetiredSourceCleanup retired;
    private final MongoTemplate mongo;
    private final String environment;
    private final MeterRegistry metrics;
    private String after;
    public RecoverySourceJobs(WithdrawalSourceCleanup cleanup, MongoSourceSweep sweep, MongoRetiredSourceCleanup retired,
            MongoTemplate mongo, String environment, MeterRegistry metrics) {
        this.cleanup = Objects.requireNonNull(cleanup); this.sweep = Objects.requireNonNull(sweep); this.retired = Objects.requireNonNull(retired);
        this.mongo = mongo; this.environment = environment; this.metrics = metrics;
    }
    @Scheduled(fixedDelay = 5000, scheduler = "recoverySourceTaskScheduler")
    public void expire() {
        try {
            var result = cleanup.batch(20, true);
            metrics.counter("identity.withdrawal.source.removed").increment(result.removed());
            metrics.counter("identity.withdrawal.source.leased").increment(result.leased());
        } catch (RuntimeException failure) { metrics.counter("identity.withdrawal.source.cleanup_failure").increment(); }
    }
    @Scheduled(fixedDelay = 5000, scheduler = "recoverySourceTaskScheduler")
    public synchronized void maintain() {
        try {
            var condition = Criteria.where("environment").is(environment).and("status").in("ACTIVE", "CLOSED_RESTORE");
            if (after != null) condition.and("_id").gt(after);
            var source = mongo.findOne(Query.query(condition).collation(Collation.of("simple"))
                    .with(Sort.by("_id")).limit(1).maxTimeMsec(5000), Document.class, "withdrawal_stream");
            if (source == null) { after = null; return; }
            if (!(source.get("_id") instanceof String id)) throw new MongoRecoveryReader.Unavailable();
            after = id; // A blocked stream cannot starve all later streams. Revisit on the next pass.
            String stream = source.getString("streamId");
            var outcome = retired.batch(environment, stream);
            if (outcome == MongoRetiredSourceCleanup.Result.NOT_DUE) sweep.batch(environment, stream);
            metrics.counter("identity.withdrawal.source.maintenance", "outcome", outcome.name()).increment();
        } catch (RuntimeException failure) { metrics.counter("identity.withdrawal.source.maintenance_failure").increment(); }
    }
}
