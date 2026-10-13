package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.MeterRegistry;

/** Independent bounded build and privacy cleanup tasks; no source journal deletion permission. */
public final class RecoverySnapshotJobs {
    private final SnapshotBuilder builder;
    private final MongoSnapshotStore store;
    private final MongoTemplate mongo;
    private final Clock clock;
    private final String environment;
    private final MeterRegistry metrics;
    private final AtomicLong privacyOverdueSeconds = new AtomicLong(-1);
    public RecoverySnapshotJobs(SnapshotBuilder builder, MongoSnapshotStore store, MongoTemplate mongo,
            Clock clock, String environment, MeterRegistry metrics) {
        this.builder = builder; this.store = store; this.mongo = mongo; this.clock = clock;
        this.environment = environment; this.metrics = metrics;
        metrics.gauge("identity.withdrawal.recovery.privacy_overdue_seconds", privacyOverdueSeconds);
    }
    @Scheduled(fixedDelay = 1000, scheduler = "recoveryTaskScheduler")
    public void build() {
        try {
            var outcome = builder.buildNext(environment);
            metrics.counter("identity.withdrawal.recovery.build", "outcome", outcome.name()).increment();
        } catch (RuntimeException failure) { metrics.counter("identity.withdrawal.recovery.build_failure").increment(); }
    }
    @Scheduled(fixedDelay = 5000, scheduler = "recoveryTaskScheduler")
    public void maintain() {
        try {
            store.maintain(100);
            var query = Query.query(Criteria.where("environment").is(environment).and("privacyCleaned").ne(true)
                    .and("privacyExpiresAt").lte(Date.from(clock.instant())))
                    .with(Sort.by("privacyExpiresAt", "_id")).limit(1).maxTimeMsec(5000);
            query.fields().include("privacyExpiresAt");
            var oldest = mongo.findOne(query, Document.class, MongoSnapshotStore.RUNS);
            long overdue = oldest == null ? 0 : Math.max(1, Duration.between(oldest.getDate("privacyExpiresAt").toInstant(), clock.instant()).getSeconds());
            privacyOverdueSeconds.set(overdue);
            if (overdue > 0) metrics.counter("identity.withdrawal.recovery.privacy_overdue").increment();
            metrics.counter("identity.withdrawal.recovery.maintenance_success").increment();
        } catch (RuntimeException failure) {
            privacyOverdueSeconds.set(-1); // unknown is not healthy zero
            metrics.counter("identity.withdrawal.recovery.maintenance_failure").increment();
        }
    }
}
