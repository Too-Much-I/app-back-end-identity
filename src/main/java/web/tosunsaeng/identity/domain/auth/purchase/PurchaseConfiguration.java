package web.tosunsaeng.identity.domain.auth.purchase;

import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.JwtProperties;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PurchaseProperties.class)
public class PurchaseConfiguration {
    @Bean
    org.springframework.beans.factory.InitializingBean validatePurchaseConfiguration(PurchaseProperties props,
            JwtProperties jwt, ObjectProvider<PurchaseReadiness> readiness,
            @Value("${app.session-revocation.fence-enabled:false}") boolean sessionFence) {
        return () -> {
            if (props.isFenceEnabled() && !sessionFence)
                throw new IllegalStateException("Purchase preparation requires the existing session fence.");
            props.validate(jwt.accessTokenTtl(), sessionFence,
                    props.isScopeEnabled() && readiness.getIfAvailable(() -> () -> false).ready());
        };
    }
    @Bean
    @ConditionalOnProperty(prefix = "app.purchase", name = "control-writes-enabled", havingValue = "true")
    PurchaseControlService purchaseControlService(MongoTemplate mongo, UserRepository users, MongoTransactionManager manager) {
        return new PurchaseControlService(mongo, users, new TransactionTemplate(manager));
    }
    @Bean
    @ConditionalOnProperty(prefix = "app.purchase", name = "control-writes-enabled", havingValue = "true")
    PurchaseControlMigration purchaseControlMigration(MongoTemplate mongo, MongoTransactionManager manager) {
        return new PurchaseControlMigration(mongo, new TransactionTemplate(manager));
    }
    @Bean
    @ConditionalOnProperty(prefix = "app.purchase", name = "fence-enabled", havingValue = "true")
    PurchaseTokenFence purchaseTokenFence(PurchaseProperties props, ObjectProvider<PurchaseReadiness> readiness,
            PurchaseControlService controls, MongoTransactionManager manager, Clock clock,
            web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository sessions,
            web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService security) {
        return new PurchaseTokenFence(props, readiness.getIfAvailable(() -> () -> false), controls,
                new TransactionTemplate(manager), clock, sessions, security);
    }
}
