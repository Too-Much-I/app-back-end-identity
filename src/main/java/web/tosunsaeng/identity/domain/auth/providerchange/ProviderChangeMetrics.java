package web.tosunsaeng.identity.domain.auth.providerchange;

import java.time.Duration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Fixed labels only. Never use a user, binding, operation ID or provider response as a metric label. */
@Component
public class ProviderChangeMetrics {
	public enum Outcome { ACCEPTED, DUPLICATE, LAST_METHOD, BLOCKED_LOGIN, STALE_SYNC, RELINKED,
		COMPLETED, SAFE_RETRY, RECONCILIATION }
	private final MeterRegistry registry;
	public ProviderChangeMetrics(MeterRegistry registry) { this.registry = registry; }
	public void record(Outcome outcome) {
		afterCommit(() -> registry.counter("identity.provider.change", "outcome", outcome.name()).increment());
	}
	public void rejected(Outcome outcome) {
		// Rejections deliberately survive transaction rollback; they count attempts, not committed changes.
		registry.counter("identity.provider.change.rejected", "outcome", outcome.name()).increment();
	}
	public void pendingAge(Duration age) {
		if (!age.isNegative()) registry.summary("identity.provider.change.pending.age.seconds").record(age.toSeconds());
	}
	public void phaseLatency(ProviderUnlinkOperation.State phase, Duration age) {
		if (!age.isNegative()) afterCommit(() -> registry.timer("identity.provider.change.phase", "phase", phase.name()).record(age));
	}
	private void afterCommit(Runnable record) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override public void afterCommit() { record.run(); }
			});
		} else record.run();
	}
}
