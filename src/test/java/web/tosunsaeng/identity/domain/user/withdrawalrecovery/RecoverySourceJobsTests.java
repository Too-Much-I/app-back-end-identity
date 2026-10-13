package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.*;

class RecoverySourceJobsTests {
    final WithdrawalSourceCleanup cleanup = mock(WithdrawalSourceCleanup.class);
    final MongoSourceSweep sweep = mock(MongoSourceSweep.class);
    final MongoRetiredSourceCleanup retired = mock(MongoRetiredSourceCleanup.class);
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    final RecoverySourceJobs jobs = new RecoverySourceJobs(cleanup, sweep, retired, mongo, "test", metrics);
    @Test void jobsOffNeedsNoRuntimeOrApproval() {
        new ApplicationContextRunner().withUserConfiguration(RecoverySourceJobsConfiguration.class)
                .run(c -> { assertThat(c).hasNotFailed(); assertThat(c).doesNotHaveBean(RecoverySourceJobs.class); });
    }
    @Test void onWithoutEvidenceFailsStartup() {
        new ApplicationContextRunner().withUserConfiguration(RecoverySourceJobsConfiguration.class)
                .withPropertyValues("app.withdrawal-recovery.source-jobs-enabled=true").run(c -> assertThat(c).hasFailed());
    }
    @Test void expiryIsBoundedAndFailureIsCountedWithoutRethrowing() {
        when(cleanup.batch(20, true)).thenReturn(new WithdrawalSourceCleanup.Result(20, 4, 2));
        jobs.expire(); assertThat(metrics.counter("identity.withdrawal.source.removed").count()).isEqualTo(4);
        when(cleanup.batch(20, true)).thenThrow(new IllegalStateException("synthetic"));
        jobs.expire(); assertThat(metrics.counter("identity.withdrawal.source.cleanup_failure").count()).isEqualTo(1);
    }
    @Test void liveStreamSweepsButReferencedRetiredStreamDoesNot() {
        var source = RecoveryIndexesTests.stream(); String stream = source.getString("streamId");
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenReturn(source);
        when(retired.batch("test", stream)).thenReturn(MongoRetiredSourceCleanup.Result.NOT_DUE);
        jobs.maintain(); verify(sweep).batch("test", stream);
        when(retired.batch("test", stream)).thenReturn(MongoRetiredSourceCleanup.Result.REFERENCED);
        jobs.maintain(); verify(sweep, times(1)).batch(anyString(), anyString());
    }
    @Test void configurationLossFailsClosed() {
        var capture = new WithdrawalCaptureProperties(); capture.setRequired(true); capture.setCleanupEnabled(true);
        assertThatThrownBy(() -> RecoverySourceJobsConfiguration.requireConfiguration(capture)).isInstanceOf(IllegalStateException.class);
        capture.setEnvironment("test");
        RecoverySourceJobsConfiguration.requireConfiguration(capture);
        capture.setCleanupEnabled(false);
        assertThatThrownBy(() -> RecoverySourceJobsConfiguration.requireConfiguration(capture)).isInstanceOf(IllegalStateException.class);
    }
    @Test void onNeedsNoExternalApprovalOrRuntimeEvidenceButRunsPreflight() {
        var capture = new WithdrawalCaptureProperties(); capture.setRequired(true); capture.setCleanupEnabled(true); capture.setEnvironment("test");
        when(mongo.getMongoDatabaseFactory()).thenReturn(mock(org.springframework.data.mongodb.MongoDatabaseFactory.class));
        try (var indexes = mockConstruction(RecoveryIndexes.class)) {
        new ApplicationContextRunner().withUserConfiguration(RecoverySourceJobsConfiguration.class)
                .withPropertyValues("app.withdrawal-recovery.source-jobs-enabled=true")
                .withBean(MongoTemplate.class, () -> mongo).withBean(WithdrawalCaptureProperties.class, () -> capture)
                .withBean(WithdrawalSourceCleanup.class, () -> cleanup)
                .withBean(java.time.Clock.class, java.time.Clock::systemUTC)
                .withBean(io.micrometer.core.instrument.MeterRegistry.class, () -> metrics)
                .run(c -> {
                    assertThat(c).hasNotFailed();
                    assertThat(c).hasSingleBean(RecoverySourceJobs.class);
                    assertThat(indexes.constructed()).hasSize(2);
                    verify(indexes.constructed().get(0)).verify();
                    verify(indexes.constructed().get(1)).verifyMigration("test");
                });
        }
    }
    @Test void wrongEnvironmentAndMalformedStreamAreRejected() {
        var capture = new WithdrawalCaptureProperties(); capture.setRequired(true); capture.setCleanupEnabled(true); capture.setEnvironment("test");
        var stream = RecoveryIndexesTests.stream().getString("streamId");
        RecoverySourceJobsConfiguration.requireScope(capture, "test", stream);
        assertThatThrownBy(() -> RecoverySourceJobsConfiguration.requireScope(capture, "production", stream)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> RecoverySourceJobsConfiguration.requireScope(capture, "test", "invalid")).isInstanceOf(RuntimeException.class);
    }
    @Test void invalidIndexesStillPreventStartup() {
        var capture = new WithdrawalCaptureProperties(); capture.setRequired(true); capture.setCleanupEnabled(true); capture.setEnvironment("test");
        try (var indexes = mockConstruction(RecoveryIndexes.class, (mock, context) -> doThrow(new IllegalStateException("invalid indexes")).when(mock).verify())) {
            assertThatThrownBy(() -> new RecoverySourceJobsConfiguration().recoverySourceExpiryEvidence(mongo, capture))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void blockedStreamAdvancesHintAndEndOfPassResetsIt() {
        var source = RecoveryIndexesTests.stream(); var stream = source.getString("streamId");
        when(mongo.findOne(any(Query.class), eq(Document.class), eq("withdrawal_stream"))).thenReturn(source, null, source);
        when(retired.batch("test", stream)).thenThrow(new SecurityException());
        jobs.maintain(); jobs.maintain(); jobs.maintain();
        var queries = org.mockito.ArgumentCaptor.forClass(Query.class);
        verify(mongo, times(3)).findOne(queries.capture(), eq(Document.class), eq("withdrawal_stream"));
        assertThat(queries.getAllValues().get(0).getQueryObject()).doesNotContainKey("_id");
        assertThat(queries.getAllValues().get(1).getQueryObject().get("_id")).isEqualTo(new Document("$gt", source.get("_id")));
        assertThat(queries.getAllValues().get(2).getQueryObject()).doesNotContainKey("_id");
        verifyNoInteractions(sweep); assertThat(metrics.counter("identity.withdrawal.source.maintenance_failure").count()).isEqualTo(2);
    }
}
