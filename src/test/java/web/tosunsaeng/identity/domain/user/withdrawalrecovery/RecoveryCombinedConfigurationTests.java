package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.*;
import com.mongodb.client.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.*;
import web.tosunsaeng.identity.domain.auth.purchase.PurchaseReadiness;

class RecoveryCombinedConfigurationTests {
    final MongoTemplate mongo = mock(MongoTemplate.class);
    final MongoDatabaseFactory factory = mock(MongoDatabaseFactory.class);
    final WithdrawalRecords.Stream source = new WithdrawalRecords.Stream("source", "00000000-0000-4000-8000-000000000001",
            "test", "ACTIVE", 1, Instant.parse("2026-10-12T00:00:00Z"), 0);
    ApplicationContextRunner runner() {
        when(mongo.getMongoDatabaseFactory()).thenReturn(factory);
        when(mongo.getDb()).thenReturn(mock(MongoDatabase.class, RETURNS_SELF));
        when(mongo.exists(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(true);
        when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(List.of(source));
        return new ApplicationContextRunner().withUserConfiguration(WithdrawalCaptureConfiguration.class,
                RecoveryRuntimeConfiguration.class, RecoverySourceJobsConfiguration.class,
                web.tosunsaeng.identity.domain.auth.purchase.PurchaseConfiguration.class)
                .withBean(web.tosunsaeng.identity.global.security.jwt.JwtProperties.class, () ->
                        new web.tosunsaeng.identity.global.security.jwt.JwtProperties("https://identity.example.test",
                                "aud", "synthetic", Duration.ofMinutes(30), "unused", "unused", List.of("billing:read")))
                .withBean(web.tosunsaeng.identity.domain.user.domain.repository.UserRepository.class, () -> mock(web.tosunsaeng.identity.domain.user.domain.repository.UserRepository.class))
                .withBean(web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository.class, () -> mock(web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository.class))
                .withBean(web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService.class, () -> mock(web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService.class))
                .withBean(MongoTemplate.class, () -> mongo).withBean(MongoClient.class, () -> mock(MongoClient.class))
                .withBean(MongoTransactionManager.class, () -> new MongoTransactionManager(factory))
                .withBean(Clock.class, () -> RecoveryRuntimeTests.CLOCK)
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("app.withdrawal-capture.required=true", "app.withdrawal-capture.environment=test",
                        "app.purchase.control-writes-enabled=true", "app.session-revocation.fence-enabled=true",
                        "app.withdrawal-capture.cleanup-enabled=true", "app.withdrawal-recovery.source-jobs-enabled=true",
                        "app.withdrawal-recovery.runtime-enabled=true", "app.withdrawal-recovery.cursor-active-key=test-key",
                        "app.withdrawal-recovery.cursor-keyring-json=" + RecoveryRuntimeTests.KEYS);
    }
    @Test void captureRecoveryAndCleanupStartWithoutExternalEvidenceButPurchaseStaysDenied() {
        try (var captureIndexes = mockConstruction(WithdrawalIndexes.class); var recoveryIndexes = mockConstruction(RecoveryIndexes.class)) {
            runner().run(c -> {
                assertThat(c).hasNotFailed().hasSingleBean(WithdrawalCaptureService.class)
                        .hasSingleBean(RecoverySnapshotService.class).hasSingleBean(RecoveryFeedService.class)
                        .hasSingleBean(RecoverySourceJobs.class)
                        .doesNotHaveBean(BillingWithdrawalPublisher.class);
                assertThat(c.getBean(PurchaseReadiness.class).ready()).isFalse();
                assertThat(captureIndexes.constructed()).isNotEmpty();
                assertThat(recoveryIndexes.constructed()).isNotEmpty();
            });
        }
    }
    @Test void missingSourcePreventsCombinedStartup() {
        try (var captureIndexes = mockConstruction(WithdrawalIndexes.class); var recoveryIndexes = mockConstruction(RecoveryIndexes.class)) {
            var runner = runner(); when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(List.of());
            runner.run(c -> assertThat(c).hasFailed());
        }
    }
    @Test void purchaseOnWithMatchingSourceStartsWithoutExternalProof() {
        try (var captureIndexes = mockConstruction(WithdrawalIndexes.class); var recoveryIndexes = mockConstruction(RecoveryIndexes.class)) {
            runner().withPropertyValues("app.purchase.fence-enabled=true", "app.purchase.scope-enabled=true",
                    "app.purchase.source-stream-id=" + source.streamId()).run(c -> {
                assertThat(c).hasNotFailed().hasSingleBean(web.tosunsaeng.identity.domain.auth.purchase.PurchaseTokenFence.class);
                assertThat(c.getBean(PurchaseReadiness.class).ready()).isTrue();
            });
        }
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"", "invalid", "00000000-0000-4000-8000-000000000002"})
    void purchaseOnWithMissingInvalidOrDifferentBindingCannotStart(String binding) {
        try (var captureIndexes = mockConstruction(WithdrawalIndexes.class); var recoveryIndexes = mockConstruction(RecoveryIndexes.class)) {
            runner().withPropertyValues("app.purchase.fence-enabled=true", "app.purchase.scope-enabled=true",
                    "app.purchase.source-stream-id=" + binding).run(c -> assertThat(c).hasFailed());
        }
    }
    @Test void missingControlFencePreventsCombinedStartup() {
        try (var captureIndexes = mockConstruction(WithdrawalIndexes.class); var recoveryIndexes = mockConstruction(RecoveryIndexes.class)) {
            runner().withPropertyValues("app.purchase.control-writes-enabled=false").run(c -> assertThat(c).hasFailed());
        }
    }
    @Test void readinessRechecksSourceAndDoesNotSwallowDatabaseFailures() {
        runner();
        var p = new WithdrawalCaptureProperties(); p.setRequired(true); p.setEnvironment("test");
        var readiness = new CaptureReadiness(mongo, p);
        assertThat(readiness.ready()).isTrue();
        when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(List.of(source, source));
        assertThat(readiness.ready()).isFalse();
        when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(List.of());
        assertThat(readiness.ready()).isFalse();
        when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenThrow(new IllegalStateException("synthetic database failure"));
        assertThatThrownBy(readiness::ready).isInstanceOf(IllegalStateException.class);
    }
    @Test void malformedSourceAndDisabledCaptureAreNotReady() {
        runner(); var p = new WithdrawalCaptureProperties(); p.setRequired(true); p.setEnvironment("test");
        var readiness = new CaptureReadiness(mongo, p);
        for (var bad : List.of(
                new WithdrawalRecords.Stream("s", "bad", "test", "ACTIVE", 1, source.captureCutover(), 0),
                new WithdrawalRecords.Stream("s", source.streamId(), "test", "ACTIVE", 1, null, 0),
                new WithdrawalRecords.Stream("s", source.streamId(), "test", "ACTIVE", 1, source.captureCutover(), 2))) {
            when(mongo.find(any(Query.class), eq(WithdrawalRecords.Stream.class))).thenReturn(List.of(bad));
            assertThat(readiness.ready()).isFalse();
        }
        p.setRequired(false); clearInvocations(mongo);
        assertThat(readiness.ready()).isFalse(); verifyNoInteractions(mongo);
    }
}
