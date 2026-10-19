package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties({WithdrawalCaptureProperties.class, web.tosunsaeng.identity.domain.auth.purchase.PurchaseProperties.class})
public class WithdrawalCaptureConfiguration {
    @Configuration(proxyBeanMethods=false)
    @EnableScheduling
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    static class MonitorConfiguration {
        @Bean WithdrawalMonitor withdrawalMonitor(MongoTemplate mongo, Clock clock, MeterRegistry metrics) {
            return new WithdrawalMonitor(mongo,clock,metrics);
        }
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    web.tosunsaeng.identity.domain.auth.purchase.PurchaseReadiness withdrawalPurchaseReadiness(
            CaptureReadiness capture, web.tosunsaeng.identity.domain.auth.purchase.PurchaseProperties purchase,
            @Value("${app.session-revocation.fence-enabled:false}") boolean sessionFence) {
        return new web.tosunsaeng.identity.domain.auth.purchase.SourceBoundPurchaseReadiness(capture,purchase,sessionFence);
    }
    @Bean org.springframework.beans.factory.InitializingBean withdrawalFlags(WithdrawalCaptureProperties p,
            @Value("${app.purchase.control-writes-enabled:false}") boolean controls,
            @Value("${app.session-revocation.fence-enabled:false}") boolean sessions) {
        return () -> p.validate(controls, sessions);
    }
    @Bean
    @org.springframework.context.annotation.Profile("!test")
    org.springframework.beans.factory.InitializingBean withdrawalCutoverGuard(MongoTemplate mongo, WithdrawalCaptureProperties p) {
        return () -> {
            // Dedicated environment DB: a durable marker cannot be bypassed by turning both flags off.
            boolean cutoverExists = mongo.exists(new Query(), Stream.class);
            boolean active = mongo.exists(Query.query(Criteria.where("environment").is(p.getEnvironment())
                    .and("status").is("ACTIVE")), Stream.class);
            if (cutoverExists && !p.isRequired()) throw new IllegalStateException("Capture cannot be disabled after cutover.");
            if (p.isRequired()) {
                new WithdrawalIndexes(mongo).verify();
                if (!active) throw new IllegalStateException("Approved capture cutover missing.");
            }
        };
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    CaptureReadiness captureReadiness(MongoTemplate mongo, WithdrawalCaptureProperties p) {
        new WithdrawalIndexes(mongo).verify();
        var readiness = new CaptureReadiness(mongo, p);
        if (!readiness.ready()) throw new IllegalStateException("Capture database/configuration is not ready.");
        return readiness;
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    WithdrawalCaptureService withdrawalCaptureService(MongoTemplate mongo, WithdrawalCaptureProperties p, Clock clock,
            CaptureReadiness readiness) {
        if (!readiness.ready()) throw new IllegalStateException("Capture database/configuration is not ready.");
        return new WithdrawalCaptureService(mongo,p.getEnvironment(),clock,readiness::ready);
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    WithdrawalDeliveryStore withdrawalDeliveryStore(MongoTemplate mongo) { return new WithdrawalDeliveryStore(mongo); }

    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="cleanup-enabled",havingValue="true")
    WithdrawalSourceCleanup withdrawalSourceCleanup(MongoTemplate mongo, WithdrawalDeliveryStore store,
            WithdrawalSourceCleanup.Evidence evidence, Clock clock) {
        var tx = new TransactionTemplate(new MongoTransactionManager(mongo.getMongoDatabaseFactory(),
                com.mongodb.TransactionOptions.builder().readConcern(com.mongodb.ReadConcern.SNAPSHOT)
                        .writeConcern(com.mongodb.WriteConcern.MAJORITY).readPreference(com.mongodb.ReadPreference.primary()).build()));
        tx.setTimeout(30);
        return new WithdrawalSourceCleanup(mongo,store,tx,evidence,clock);
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    WithdrawalOperations withdrawalOperations(WithdrawalDeliveryStore store, MongoTransactionManager manager,
            ObjectProvider<WithdrawalOperations.Authority> authority, Clock clock, WithdrawalCaptureProperties p) {
        WithdrawalOperations.Authority denied = new WithdrawalOperations.Authority() {
            public void requireResumePermission() { throw new SecurityException("Resume authority unavailable."); }
            public void auditResume(String op,String reason,String version,int count) { throw new SecurityException("Audit unavailable."); }
        };
        return new WithdrawalOperations(store,authority.getIfAvailable(() -> denied),new TransactionTemplate(manager),clock,p.getEnvironment());
    }
    @Configuration(proxyBeanMethods=false)
    @EnableScheduling
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="publisher-enabled",havingValue="true")
    static class PublisherConfiguration {
        @Bean BillingWithdrawalPublisher billingWithdrawalPublisher(WithdrawalDeliveryStore store,
                web.tosunsaeng.identity.global.workload.BillingJwtJsonTransport.Factory jwt,
                WithdrawalCaptureProperties p, Clock clock, ObjectMapper mapper, MeterRegistry metrics) {
            var transport = jwt.create(p.getBillingOrigin(), Duration.ofSeconds(3), Duration.ofSeconds(5));
            return new BillingWithdrawalPublisher(store,body -> {
                var response = transport.postDetailed("/internal/v1/payments/accounts/withdrawal/events",body);
                return new BillingWithdrawalPublisher.Response(response.status(),response.retryAfter());
            },new BillingWithdrawalRetry(() -> ThreadLocalRandom.current().nextDouble()),mapper,clock,
                    p::isPublisherEnabled,p.getEnvironment(),metrics);
        }
        @Bean PublisherScheduler billingWithdrawalScheduler(BillingWithdrawalPublisher publisher, MeterRegistry metrics) {
            return new PublisherScheduler(publisher, metrics);
        }
    }
    static final class PublisherScheduler {
        private final BillingWithdrawalPublisher publisher;
        private final MeterRegistry metrics;
        PublisherScheduler(BillingWithdrawalPublisher publisher, MeterRegistry metrics) { this.publisher=publisher; this.metrics=metrics; }
        @Scheduled(fixedDelay=5000)
        public void poll() {
            try { for (int i=0;i<20 && publisher.publishNext();i++) { /* claim only just before sending */ } }
            catch (RuntimeException failure) { metrics.counter("identity.withdrawal.billing.poll_failure").increment(); }
        }
    }
}
