package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(name = "app.firebase-auth.enabled", havingValue = "true")
public class PhoneOnlyCleanupFenceConfiguration {
    @Bean
    PhoneOnlyCleanupFence phoneOnlyCleanupFence(MongoTemplate mongo, MongoTransactionManager manager,
                                               Clock clock, FirebaseAuthProperties firebase) {
        mongo.indexOps(PhoneOnlyCleanupFence.COLLECTION).ensureIndex(new Index().on("expiresAt", Sort.Direction.ASC)
                .expire(Duration.ZERO).named("ttl_phone_only_cleanup_terminal"));
        var tx = new TransactionTemplate(manager);
        tx.setTimeout(10);
        return new PhoneOnlyCleanupFence(mongo, tx, clock, firebase.tenantId() == null ? "" : firebase.tenantId());
    }
}
