package web.tosunsaeng.identity.domain.auth.purchase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.security.jwt.JwtProperties;

class PurchaseConfigurationTests {
    final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PurchaseConfiguration.class)
            .withBean(JwtProperties.class, () -> new JwtProperties("https://identity.example.test", "aud", "synthetic",
                    Duration.ofMinutes(30), "unused", "unused", List.of("billing:read")))
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(MongoTemplate.class, () -> mock(MongoTemplate.class))
            .withBean(MongoTransactionManager.class, () -> mock(MongoTransactionManager.class))
            .withBean(UserRepository.class, () -> mock(UserRepository.class))
            .withBean(RefreshSessionRepository.class, () -> mock(RefreshSessionRepository.class))
            .withBean(SessionSecurityService.class, () -> mock(SessionSecurityService.class));
    @Test void defaultDoesNotInstallWriterOrFenceAndMakesNoMongoCalls() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(PurchaseTokenFence.class)
                    .doesNotHaveBean(PurchaseControlService.class);
            assertOnlyContextInjection(context.getBean(MongoTemplate.class));
        });
    }
    @Test void writerPreparationDoesNotActivateStrictIssuance() {
        runner.withPropertyValues("app.purchase.control-writes-enabled=true").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PurchaseControlService.class)
                    .doesNotHaveBean(PurchaseTokenFence.class);
            assertOnlyContextInjection(context.getBean(MongoTemplate.class)); // no automatic migration
        });
    }
    @Test void scopeCannotBeEnabledWithoutRealCaptureReadinessAdapter() {
        runner.withPropertyValues("app.purchase.control-writes-enabled=true", "app.purchase.fence-enabled=true",
                "app.purchase.scope-enabled=true", "app.session-revocation.fence-enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }
    @Test void readyAdapterAllowsTestActivationButDoesNotWriteDbOnStartup() {
        runner.withBean(PurchaseReadiness.class, () -> () -> true)
                .withPropertyValues("app.purchase.control-writes-enabled=true", "app.purchase.fence-enabled=true",
                        "app.purchase.scope-enabled=true", "app.session-revocation.fence-enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(PurchaseTokenFence.class);
                    assertOnlyContextInjection(context.getBean(MongoTemplate.class));
                });
    }
    private static void assertOnlyContextInjection(MongoTemplate mongo) {
        verify(mongo).setApplicationContext(any()); // Spring lifecycle, not a database call
        verifyNoMoreInteractions(mongo);
    }
}
