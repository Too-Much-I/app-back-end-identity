package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Date;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RecoverySnapshotJobsTests {
    final SnapshotBuilder builder = mock(SnapshotBuilder.class);
    final MongoSnapshotStore store = mock(MongoSnapshotStore.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    final RecoverySnapshotJobs jobs = new RecoverySnapshotJobs(builder, store, mongo, RecoveryRuntimeTests.CLOCK, "test", metrics);
    @Test void buildIsSingleBoundedCallAndRecordsOutcome() {
        when(builder.buildNext("test")).thenReturn(SnapshotBuilder.Outcome.READY); jobs.build();
        verify(builder, times(1)).buildNext("test"); verifyNoInteractions(store, mongo);
        assertThat(metrics.get("identity.withdrawal.recovery.build").tag("outcome", "READY").counter().count()).isEqualTo(1);
    }
    @Test void buildFailureDoesNotPreventIndependentMaintenance() {
        when(builder.buildNext("test")).thenThrow(new IllegalStateException("synthetic")); jobs.build(); jobs.maintain();
        verify(store).maintain(100);
        assertThat(metrics.get("identity.withdrawal.recovery.build_failure").counter().count()).isEqualTo(1);
    }
    @Test void maintenanceUsesBoundedBatchAndFlagsOverdue() {
        when(mongo.findOne(any(Query.class), eq(Document.class), eq(MongoSnapshotStore.RUNS))).thenReturn(
                new Document("privacyExpiresAt", Date.from(RecoveryRuntimeTests.CLOCK.instant().minusSeconds(60))));
        jobs.maintain(); verify(store).maintain(100);
        assertThat(metrics.get("identity.withdrawal.recovery.privacy_overdue_seconds").gauge().value()).isEqualTo(60);
        assertThat(metrics.get("identity.withdrawal.recovery.privacy_overdue").counter().count()).isEqualTo(1);
    }
    @Test void failedMaintenanceIsUnknownNotHealthyZero() {
        doThrow(new IllegalStateException()).when(store).maintain(100); jobs.maintain();
        assertThat(metrics.get("identity.withdrawal.recovery.privacy_overdue_seconds").gauge().value()).isEqualTo(-1);
        assertThat(metrics.get("identity.withdrawal.recovery.maintenance_failure").counter().count()).isEqualTo(1);
        verifyNoInteractions(mongo);
    }
    @Test void noExpiredWorkIsHealthyZero() {
        jobs.maintain(); assertThat(metrics.get("identity.withdrawal.recovery.privacy_overdue_seconds").gauge().value()).isZero();
    }
}
