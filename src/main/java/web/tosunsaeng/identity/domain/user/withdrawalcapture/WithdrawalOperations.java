package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import org.springframework.transaction.support.TransactionTemplate;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;

/** Internal command service only. No public endpoint or permissive authorization bean. */
public final class WithdrawalOperations {
    public interface Authority {
        void requireResumePermission();
        void auditResume(String operationId, String reasonCode, String configurationVersion, int count);
    }
    private final WithdrawalDeliveryStore store;
    private final Authority authority;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final String environment;
    public WithdrawalOperations(WithdrawalDeliveryStore store, Authority authority, TransactionTemplate transactions,
            Clock clock, String environment) {
        this.store=store; this.authority=authority; this.transactions=transactions; this.clock=clock; this.environment=environment;
    }
    public boolean resume(String eventId, long expectedGeneration, String operationId, String reason, String configVersion) {
        authority.requireResumePermission();
        uuid(eventId); uuid(operationId);
        if (!Set.of("AUTH_FIXED", "CONTRACT_FIXED", "TRANSIENT_RESOLVED", "CLOCK_FIXED", "HEADER_REVIEWED").contains(reason)
                || configVersion == null || !configVersion.matches("[A-Za-z0-9._-]{1,64}")
                || expectedGeneration < 0 || expectedGeneration == Long.MAX_VALUE)
            throw new IllegalArgumentException("Invalid resume command.");
        return Boolean.TRUE.equals(transactions.execute(tx -> {
            Delivery d = store.find(eventId);
            if (d == null) throw new IllegalStateException("Source unavailable.");
            if (operationId.equals(d.resumeOperationId())) {
                if (d.resumeGeneration() == expectedGeneration + 1 && reason.equals(d.resumeReason())
                        && configVersion.equals(d.resumeConfigVersion())) return false;
                throw new IllegalStateException("Resume operation binding conflict.");
            }
            Journal j = store.journal(d);
            Instant now = clock.instant();
            if (j == null || !now.isBefore(j.expiresAt()) || !store.activeStream(j, environment)
                    || d.resumeGeneration() != expectedGeneration || d.leaseVersion() == Long.MAX_VALUE
                    || d.notBefore() == null || now.isBefore(d.notBefore()))
                throw new IllegalStateException("Resume precondition failed.");
            if (!store.resume(d, operationId, reason, configVersion, now)) throw new IllegalStateException("Resume conflict.");
            // Adapter must write the audit durably in this same transaction; failure rolls back resume.
            authority.auditResume(operationId, reason, configVersion, 1);
            return true;
        }));
    }
}
