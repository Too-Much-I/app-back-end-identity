package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.apache.catalina.connector.*;
import org.apache.catalina.valves.ValveBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;

class RecoveryWorkloadConfigurationTests {
    @Test void offDoesNotNeedKeysPortsOrTlsAndDoesNotInstallConnector() {
        new ApplicationContextRunner().withUserConfiguration(RecoveryWorkloadConfiguration.class).run(c -> {
            assertThat(c).hasNotFailed(); assertThat(c).doesNotHaveBean(RecoveryIngressVerifier.class);
            assertThat(c).doesNotHaveBean("recoveryConnector");
        });
    }
    @Test void onWithoutTrustConfigurationFailsClosed() {
        new ApplicationContextRunner().withUserConfiguration(RecoveryWorkloadConfiguration.class)
                .withPropertyValues("app.withdrawal-recovery.http-enabled=true").run(c -> assertThat(c).hasFailed());
    }
    @Test void rejectsSharedPortAndContextPath() {
        var config = new RecoveryWorkloadConfiguration();
        var customize = config.recoveryConnector(19443, "synthetic-cert.pem", "synthetic-key.pem");
        var factory = new TomcatServletWebServerFactory(19443);
        assertThatThrownBy(() -> customize.customize(factory)).isInstanceOf(IllegalArgumentException.class);
        factory.setPort(8081); factory.setContextPath("/identity");
        assertThatThrownBy(() -> customize.customize(factory)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void connectorIsHttpsOnlyAndDoesNotChangePublicConnector() {
        var factory = new TomcatServletWebServerFactory(8081);
        new RecoveryWorkloadConfiguration().recoveryConnector(19443, "synthetic-cert.pem", "synthetic-key.pem").customize(factory);
        assertThat(factory.getPort()).isEqualTo(8081);
        assertThat(factory.getAdditionalTomcatConnectors()).hasSize(1);
        var connector = factory.getAdditionalTomcatConnectors().getFirst();
        assertThat(connector.getPort()).isEqualTo(19443);
        assertThat(connector.getScheme()).isEqualTo("https"); assertThat(connector.getSecure()).isTrue();
        assertThat(connector.getProperty("SSLEnabled")).isEqualTo(true);
    }
    @Test void valveUsesConnectorInsteadOfForwardedLocalPort() throws Exception {
        var valve = new RecoveryWorkloadConfiguration.PortBoundary(19443);
        var calls = new AtomicInteger();
        valve.setNext(new ValveBase(true) {
            @Override public void invoke(Request r, Response s) throws IOException, ServletException { calls.incrementAndGet(); }
        });
        Connector connector = new Connector(); connector.setPort(8081);
        var coyote = new org.apache.coyote.Request(); coyote.method().setString("POST");
        coyote.decodedURI().setString(RecoveryProtocol.Route.CREATE.path()); coyote.setLocalPort(19443);
        coyote.getMimeHeaders().addValue("X-Forwarded-Port").setString("19443");
        var request = new Request(connector); request.setCoyoteRequest(coyote);
        var response = new Response(); response.setCoyoteResponse(new org.apache.coyote.Response());
        valve.invoke(request, response);
        assertThat(response.getStatus()).isEqualTo(403); assertThat(calls.get()).isZero();
        connector.setPort(19443); valve.invoke(request, response);
        assertThat(calls.get()).isEqualTo(1);
        assertThat(request.getAttribute(RecoveryWorkloadConfiguration.CONNECTOR_PORT)).isEqualTo(19443);
    }
}
