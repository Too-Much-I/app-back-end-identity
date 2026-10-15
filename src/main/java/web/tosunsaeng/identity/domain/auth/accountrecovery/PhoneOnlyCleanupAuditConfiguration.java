package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.Clock;
import com.google.firebase.auth.AbstractFirebaseAuth;
import com.google.firebase.auth.FirebaseAuth;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.*;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAppHandle;
import web.tosunsaeng.identity.domain.auth.federation.infrastructure.firebase.FirebaseAuthProperties;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintHasher;

@Configuration(proxyBeanMethods = false)
@Profile("prod")
@EnableScheduling
@ConditionalOnProperty(name = "app.phone-only-cleanup.audit-enabled", havingValue = "true")
public class PhoneOnlyCleanupAuditConfiguration {
    @Bean
    PhoneOnlyCleanupAudit phoneOnlyCleanupAudit(FirebaseAppHandle handle, FirebaseAuthProperties properties,
                                               MongoTemplate mongo, PhoneFingerprintHasher hasher, Clock clock) {
        if (!properties.enabled() || !properties.phoneEnabled()) {
            throw new IllegalStateException("Phone-only audit requires Firebase phone authentication");
        }
        FirebaseAuth root = FirebaseAuth.getInstance(handle.firebaseApp());
        AbstractFirebaseAuth auth = properties.tenantId() == null || properties.tenantId().isBlank()
                ? root : root.getTenantManager().getAuthForTenant(properties.tenantId());
        return new PhoneOnlyCleanupAudit(auth, mongo, hasher, clock);
    }

    @Bean
    AuditRunner phoneOnlyAuditRunner(PhoneOnlyCleanupAudit audit) { return new AuditRunner(audit); }

    static final class AuditRunner {
        private final PhoneOnlyCleanupAudit audit;
        AuditRunner(PhoneOnlyCleanupAudit audit) { this.audit = audit; }
        @Scheduled(initialDelayString = "PT5M", fixedDelayString = "PT5M")
        public void inspect() {
            var log = LoggerFactory.getLogger(AuditRunner.class);
            try {
                var report = audit.inspectPage();
                log.info("Phone-only audit: scanned={}, sweepComplete={}, decisions={}",
                        report.scanned(), report.sweepComplete(), report.counts());
            } catch (Exception failure) {
                log.warn("Phone-only audit failed; no accounts mutated");
            }
        }
    }
}
