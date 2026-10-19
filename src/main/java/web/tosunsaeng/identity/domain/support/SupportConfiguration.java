package web.tosunsaeng.identity.domain.support;

import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(SupportProperties.class)
public class SupportConfiguration {
    @Configuration(proxyBeanMethods=false)
    @ConditionalOnExpression("${app.support.enabled:false} or ${app.support.worker-enabled:false}")
    static class Storage {
        @Bean MongoSupportStore supportStore(MongoTemplate mongo, MongoTransactionManager manager) {
            var tx = new TransactionTemplate(manager); tx.setTimeout(10);
            var store = new MongoSupportStore(mongo, tx); store.ensureIndexes(); return store;
        }
    }
    @Bean
    @ConditionalOnProperty(prefix="app.support", name="enabled", havingValue="true")
    SupportService supportService(MongoSupportStore store, SupportProperties properties, ObjectMapper mapper,
            Clock clock, MeterRegistry metrics, Environment environment) {
        // Preserve the actual socket peer for the explicit trusted-proxy algorithm.
        if (!"none".equalsIgnoreCase(environment.getProperty("server.forward-headers-strategy", "none"))) {
            throw new IllegalArgumentException("Support requires server.forward-headers-strategy=none and explicit trusted proxies");
        }
        return new SupportService(store, new SupportCrypto(properties.hmacKey()), mapper, clock, metrics);
    }
    @Configuration(proxyBeanMethods=false)
    @EnableScheduling
    @ConditionalOnProperty(prefix="app.support", name="worker-enabled", havingValue="true")
    static class Delivery {
        @Bean SupportWorker supportWorker(MongoSupportStore store, SupportProperties props, ObjectMapper mapper, Clock clock, MeterRegistry metrics) {
            return new SupportWorker(store, new SlackSupportNotifier(props.slackWebhookUrl(), mapper, clock), clock, metrics);
        }
    }
}
