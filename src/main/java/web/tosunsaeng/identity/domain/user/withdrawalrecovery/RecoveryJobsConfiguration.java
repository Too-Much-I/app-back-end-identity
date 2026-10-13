package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalCaptureProperties;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "app.withdrawal-recovery", name = "snapshot-jobs-enabled", havingValue = "true")
public class RecoveryJobsConfiguration {
    @Bean(destroyMethod = "shutdown") ThreadPoolTaskScheduler recoveryTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler(); scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("withdrawal-recovery-"); scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false); return scheduler;
    }
    @Bean RecoverySnapshotJobs recoverySnapshotJobs(RecoveryRuntimeConfiguration.Runtime runtime,
            SnapshotBuilder builder, MongoTemplate mongo, Clock clock, WithdrawalCaptureProperties capture, MeterRegistry metrics) {
        return new RecoverySnapshotJobs(builder, runtime.store(), mongo, clock, capture.getEnvironment(), metrics);
    }
}
