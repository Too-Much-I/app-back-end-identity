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
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import web.tosunsaeng.identity.global.workload.BillingSigV4JsonTransport;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(WithdrawalCaptureProperties.class)
public class WithdrawalCaptureConfiguration {
    @Configuration(proxyBeanMethods=false)
    @EnableScheduling
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    static class MonitorConfiguration {
        @Bean WithdrawalMonitor withdrawalMonitor(MongoTemplate mongo, Clock clock, MeterRegistry metrics) {
            return new WithdrawalMonitor(mongo,clock,metrics);
        }
    }
    /** Production clock/cutover evidence is provided by deployment integration, not a boolean default. */
    public interface RuntimeEvidence { boolean clockTrusted(); boolean captureReady(); }
    /** I1 migration/old-writer drain plus I3 and joint token gates, bound to the current source incarnation. */
    public interface PurchaseActivationEvidence { boolean verifiedFor(String streamId); }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    web.tosunsaeng.identity.domain.auth.purchase.PurchaseReadiness withdrawalPurchaseReadiness(
            MongoTemplate mongo, WithdrawalCaptureProperties p, RuntimeEvidence runtime,
            ObjectProvider<PurchaseActivationEvidence> evidence) {
        return () -> {
            var proof = evidence.getIfAvailable();
            if (proof == null || !p.isRequired() || !runtime.captureReady() || !runtime.clockTrusted()) return false;
            var stream = mongo.findOne(Query.query(Criteria.where("environment").is(p.getEnvironment())
                    .and("status").is("ACTIVE")), Stream.class);
            return stream != null && proof.verifiedFor(stream.streamId());
        };
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
    WithdrawalCaptureService withdrawalCaptureService(MongoTemplate mongo, WithdrawalCaptureProperties p, Clock clock,
            RuntimeEvidence evidence) {
        if (!evidence.captureReady() || !evidence.clockTrusted()) throw new IllegalStateException("Capture evidence unavailable.");
        return new WithdrawalCaptureService(mongo,p.getEnvironment(),clock,() -> evidence.captureReady() && evidence.clockTrusted());
    }
    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="required",havingValue="true")
    WithdrawalDeliveryStore withdrawalDeliveryStore(MongoTemplate mongo) { return new WithdrawalDeliveryStore(mongo); }

    @Bean
    @ConditionalOnProperty(prefix="app.withdrawal-capture",name="cleanup-enabled",havingValue="true")
    WithdrawalSourceCleanup withdrawalSourceCleanup(MongoTemplate mongo, WithdrawalDeliveryStore store,
            MongoTransactionManager manager, WithdrawalSourceCleanup.Evidence evidence, Clock clock) {
        return new WithdrawalSourceCleanup(mongo,store,new TransactionTemplate(manager),evidence,clock);
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
        @Bean(destroyMethod="close") DefaultCredentialsProvider withdrawalBillingCredentials() { return DefaultCredentialsProvider.create(); }
        @Bean BillingWithdrawalPublisher billingWithdrawalPublisher(WithdrawalDeliveryStore store,
                @org.springframework.beans.factory.annotation.Qualifier("withdrawalBillingCredentials") DefaultCredentialsProvider credentials,
                WithdrawalCaptureProperties p, RuntimeEvidence evidence, Clock clock, ObjectMapper mapper, MeterRegistry metrics) {
            var transport = new BillingSigV4JsonTransport(p.getBillingOrigin(),p.getRegion(),Duration.ofSeconds(3),Duration.ofSeconds(5),credentials);
            return new BillingWithdrawalPublisher(store,body -> {
                var response = transport.postDetailed("/internal/v1/payments/accounts/withdrawal/events",body);
                return new BillingWithdrawalPublisher.Response(response.status(),response.retryAfter());
            },new BillingWithdrawalRetry(() -> ThreadLocalRandom.current().nextDouble()),mapper,clock,
                    evidence::clockTrusted,p::isPublisherEnabled,p.getEnvironment(),metrics);
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
