package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ProviderChangePolicyTests {
	@Test void requestBudgetLimitsCardinalityAndRecoversAtNextMinute() {
		var budget = new ProviderRequestBudget(2, 2);
		assertThat(budget.admit("owner-a", 1)).isTrue(); assertThat(budget.admit("owner-a", 1)).isTrue();
		assertThat(budget.admit("owner-a", 1)).isFalse(); assertThat(budget.admit("owner-b", 1)).isTrue();
		assertThat(budget.admit("owner-c", 1)).isFalse(); assertThat(budget.admit("owner-c", 2)).isTrue();
	}
	@Test void committedMetricsWaitForCommitAndRejectionsSurviveRollback() {
		var registry = new SimpleMeterRegistry();
		try {
			var metrics = new ProviderChangeMetrics(registry);
			TransactionSynchronizationManager.initSynchronization();
			try {
				metrics.record(ProviderChangeMetrics.Outcome.COMPLETED);
				metrics.rejected(ProviderChangeMetrics.Outcome.STALE_SYNC);
				assertThat(registry.find("identity.provider.change").counter()).isNull();
				assertThat(registry.get("identity.provider.change.rejected").counter().count()).isEqualTo(1);
				for (var sync : TransactionSynchronizationManager.getSynchronizations()) sync.afterCommit();
				assertThat(registry.get("identity.provider.change").counter().count()).isEqualTo(1);
				registry.getMeters().forEach(m -> assertThat(m.getId().getTags()).allMatch(t -> t.getKey().equals("outcome")));
			} finally { TransactionSynchronizationManager.clearSynchronization(); }
		} finally { registry.close(); }
	}
}
