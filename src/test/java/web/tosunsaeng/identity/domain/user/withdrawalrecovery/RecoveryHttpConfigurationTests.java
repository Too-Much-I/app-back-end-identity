package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

class RecoveryHttpConfigurationTests {
    final WebApplicationContextRunner runner = new WebApplicationContextRunner().withUserConfiguration(Base.class);
    @Configuration(proxyBeanMethods = false) @EnableWebSecurity @EnableWebMvc
    @Import(RecoveryHttpConfiguration.class) static class Base { }
    @Test void enabledWithoutTrustAdapterFailsStartup() {
        runner.withPropertyValues("app.withdrawal-recovery.http-enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage("Recovery HTTP requires explicit trusted ingress and runtime services.");
        });
    }
    @Test void disabledNeedsNoRuntimeServices() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }
    @Test void enabledChainInvokesVerifierAndDoesNotFallThroughToMvc() {
        var verifier = mock(RecoveryIngressVerifier.class);
        var snapshots = mock(RecoverySnapshotService.class);
        var feed = mock(RecoveryFeedService.class);
        when(verifier.authorizeHeaders(any(), any())).thenReturn(new RecoveryCursor.Owner("test", "billing"));
        when(snapshots.create(any(), any(), any())).thenReturn(new RecoverySnapshotService.Reply(202, new byte[]{123,125}, 5));
        runner.withPropertyValues("app.withdrawal-recovery.http-enabled=true")
                .withBean(RecoveryIngressVerifier.class, () -> verifier)
                .withBean(RecoverySnapshotService.class, () -> snapshots)
                .withBean(RecoveryFeedService.class, () -> feed).run(context -> {
                    assertThat(context).hasNotFailed();
                    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(SecurityMockMvcConfigurers.springSecurity()).build();
                    mvc.perform(post(RecoveryProtocol.Route.CREATE.path()).contentType("application/json")
                            .header("Idempotency-Key", RecoveryHttpFilterTests.ID)
                            .content("{\"streamId\":\"" + RecoveryHttpFilterTests.ID + "\"}"))
                            .andExpect(status().isAccepted());
                    verify(snapshots).create(any(), any(), any());
                    when(verifier.authorizeHeaders(any(), any())).thenThrow(new SecurityException());
                    mvc.perform(post(RecoveryProtocol.Route.CREATE.path())
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()))
                            .andExpect(status().isForbidden());
                });
    }
}
