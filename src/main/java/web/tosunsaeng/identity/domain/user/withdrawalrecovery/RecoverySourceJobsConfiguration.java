package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.*;
import com.mongodb.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.*;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.withdrawal-recovery.source-jobs-enabled", havingValue = "true")
public class RecoverySourceJobsConfiguration {
    // Infrastructure owns absolute clock accuracy and deployment/restore writer isolation.
    // These jobs enforce local configuration and database invariants, not external approval evidence.
    @Bean MongoSourceExpiryEvidence recoverySourceExpiryEvidence(MongoTemplate mongo, WithdrawalCaptureProperties capture) {
        requireConfiguration(capture);
        new RecoveryIndexes(mongo).verify(); new RecoveryIndexes(mongo).verifyMigration(capture.getEnvironment());
        return new MongoSourceExpiryEvidence(mongo, capture.getEnvironment(), new MongoSourceExpiryEvidence.Authority() {
            public void requireCleanupPermission() { requireConfiguration(capture); }
            public void requireTimingAndSource(WithdrawalRecords.Journal journal, Instant now, String status) {
                requireConfiguration(capture);
                if (now.isBefore(journal.expiresAt()) || !("ACTIVE".equals(status) || "CLOSED_RESTORE".equals(status)))
                    throw new MongoRecoveryReader.Unavailable();
            }
        });
    }
    @Bean RecoverySourceJobs recoverySourceJobs(WithdrawalSourceCleanup cleanup, MongoTemplate mongo,
            WithdrawalCaptureProperties capture, Clock clock, MeterRegistry metrics,
            MongoSourceExpiryEvidence evidence) {
        requireConfiguration(capture);
        var tx = new TransactionTemplate(new MongoTransactionManager(mongo.getMongoDatabaseFactory(), TransactionOptions.builder()
                .readConcern(ReadConcern.SNAPSHOT).writeConcern(WriteConcern.MAJORITY).readPreference(ReadPreference.primary()).build()));
        var sweep = new MongoSourceSweep(mongo, tx, (env, stream) -> requireScope(capture, env, stream));
        var retired = new MongoRetiredSourceCleanup(mongo, tx, clock, (env, stream, closed) -> {
            requireScope(capture, env, stream);
            if (closed == null || clock.instant().isBefore(closed.plus(WithdrawalRecords.RETENTION)))
                throw new MongoRecoveryReader.Unavailable();
        });
        return new RecoverySourceJobs(cleanup, sweep, retired, mongo, capture.getEnvironment(), metrics);
    }
    @Bean(destroyMethod = "shutdown") ThreadPoolTaskScheduler recoverySourceTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler(); scheduler.setPoolSize(2); scheduler.setThreadNamePrefix("withdrawal-source-");
        scheduler.setRemoveOnCancelPolicy(true); scheduler.setWaitForTasksToCompleteOnShutdown(false); return scheduler;
    }
    static void requireConfiguration(WithdrawalCaptureProperties capture) {
        if (!capture.isRequired() || !capture.isCleanupEnabled() || capture.getEnvironment() == null
                || !capture.getEnvironment().matches("[a-z0-9-]{1,40}"))
            throw new IllegalStateException("Source maintenance requires capture, cleanup and a valid environment.");
    }
    static void requireScope(WithdrawalCaptureProperties capture, String environment, String stream) {
        requireConfiguration(capture);
        if (!capture.getEnvironment().equals(environment)) throw new MongoRecoveryReader.Unavailable();
        RecoveryProtocol.uuid4(stream);
    }
}
