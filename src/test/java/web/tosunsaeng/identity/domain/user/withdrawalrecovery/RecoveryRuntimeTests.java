package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import com.mongodb.client.*;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalCaptureProperties;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.CaptureReadiness;

class RecoveryRuntimeTests {
    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC);
    static final String KEYS = "{\"test-key\":\"" + Base64.getEncoder().encodeToString(new byte[32]) + "\"}"; // synthetic only
    @Test void defaultOffCreatesNoServicesOrJobs() {
        new ApplicationContextRunner().withUserConfiguration(RecoveryRuntimeConfiguration.class, RecoveryJobsConfiguration.class)
                .run(c -> { assertThat(c).hasNotFailed(); assertThat(c).doesNotHaveBean(RecoverySnapshotService.class); assertThat(c).doesNotHaveBean(RecoverySnapshotJobs.class); });
    }
    @Test void enabledWithoutDependenciesFailsClosed() {
        new ApplicationContextRunner().withUserConfiguration(RecoveryRuntimeConfiguration.class)
                .withPropertyValues("app.withdrawal-recovery.runtime-enabled=true").run(c -> assertThat(c).hasFailed());
    }
    @Test void jobsWithoutRuntimeFailClosed() {
        new ApplicationContextRunner().withUserConfiguration(RecoveryJobsConfiguration.class)
                .withPropertyValues("app.withdrawal-recovery.snapshot-jobs-enabled=true").run(c -> assertThat(c).hasFailed());
    }
    @Test void keyParserRejectsInvalidAndDuplicateMaterialWithoutCauseOrEcho() {
        for (String json : List.of("", "private-bad-input", "{}", "[]", "{\"key\":null}", "{\"key\":\"invalid-base64\"}", KEYS + "{}", KEYS.replace("}", ",\"test-key\":\"duplicate\"}"))) {
            assertThatThrownBy(() -> RecoveryRuntimeConfiguration.cursor(json, "test-key", CLOCK))
                    .isInstanceOf(IllegalStateException.class).hasMessage("Recovery cursor key configuration is invalid.").hasNoCause();
        }
    }
    @Test void dedicatedKeyRingAcceptsSyntheticMaterialButRequiresActiveKey() {
        assertThat(RecoveryRuntimeConfiguration.cursor(KEYS, "test-key", CLOCK)).isNotNull();
        assertThatThrownBy(() -> RecoveryRuntimeConfiguration.cursor(KEYS, "absent", CLOCK)).isInstanceOf(IllegalStateException.class);
    }
    @Test void preflightRunsBeforeRuntimeServicesAreBuilt() {
        var mongo = mock(MongoTemplate.class); var client = mock(MongoClient.class);
        var db = mock(MongoDatabase.class, RETURNS_SELF);
        when(mongo.getDb()).thenReturn(db); when(mongo.getMongoDatabaseFactory()).thenReturn(mock(MongoDatabaseFactory.class));
        var capture = new WithdrawalCaptureProperties(); capture.setRequired(true); capture.setEnvironment("test");
        var evidence = mock(CaptureReadiness.class); when(evidence.ready()).thenReturn(true);
        var config = new RecoveryRuntimeConfiguration();
        try (var indexes = mockConstruction(RecoveryIndexes.class)) {
            var runtime = config.recoveryRuntime(mongo, client, CLOCK, capture, evidence, "test-key", KEYS);
            verify(indexes.constructed().getFirst()).verify(); verify(indexes.constructed().getFirst()).verifyMigration("test");
            assertThat(config.recoverySnapshotService(runtime)).isNotNull(); assertThat(config.recoveryFeedService(runtime)).isNotNull();
            assertThat(config.recoverySnapshotBuilder(runtime)).isNotNull();
        }
    }
    @Test void untrustedEvidenceNeverReachesDatabase() {
        var mongo = mock(MongoTemplate.class); var capture = new WithdrawalCaptureProperties();
        capture.setRequired(true); capture.setEnvironment("test");
        assertThatThrownBy(() -> new RecoveryRuntimeConfiguration().recoveryRuntime(mongo, mock(MongoClient.class), CLOCK,
                capture, mock(CaptureReadiness.class), "test-key", KEYS)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mongo);
    }
}
