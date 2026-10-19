package web.tosunsaeng.identity.domain.auth.providerchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import web.tosunsaeng.identity.support.LogCapture;

class ProviderSchedulerDiagnosticTests {
	@Test void schedulerClassifiesDatabaseFailureWithoutLoggingMessage() {
		var worker = mock(ProviderUnlinkWorker.class);
		doThrow(new DataAccessResourceFailureException("test-sensitive-sentinel")).when(worker).runBatch();
		try (var logs = LogCapture.forClass(ProviderChangeConfiguration.Scheduler.class)) {
			new ProviderChangeConfiguration.Scheduler(worker).run();
			assertThat(logs.events()).singleElement().satisfies(event -> {
				assertThat(LogCapture.value(event, "failureKind")).isEqualTo("DB_UNAVAILABLE");
				assertThat(LogCapture.value(event, "operation")).isEqualTo("PROVIDER_CHANGE_BATCH");
				assertThat(event.getThrowableProxy()).isNull();
				assertThat(LogCapture.rendered(event)).doesNotContain("test-sensitive-sentinel");
			});
		}
	}
}
