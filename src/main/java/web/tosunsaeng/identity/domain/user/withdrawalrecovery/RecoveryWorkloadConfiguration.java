package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import jakarta.servlet.ServletException;
import org.apache.catalina.connector.*;
import org.apache.catalina.valves.ValveBase;
import org.apache.tomcat.util.net.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.*;

/** OFF by default. Separate HTTPS connector; certificate provisioning is a deployment responsibility. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "app.withdrawal-recovery.http-enabled", havingValue = "true")
public class RecoveryWorkloadConfiguration {
    static final String CONNECTOR_PORT = RecoveryWorkloadConfiguration.class.getName() + ".connectorPort";

    @Bean RecoveryIngressVerifier recoveryJwtVerifier(
            @Value("${app.withdrawal-recovery.jwt.public-jwks}") String jwks,
            @Value("${app.withdrawal-recovery.jwt.issuer}") String issuer,
            @Value("${app.withdrawal-recovery.jwt.audience}") String audience,
            web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalCaptureProperties capture,
            @Value("${app.withdrawal-recovery.workload-port}") int port) {
        return new RecoveryJwtVerifier(jwks, issuer, audience, new RecoveryCursor.Owner(capture.getEnvironment(), "billing"), Clock.systemUTC(), port);
    }

    @Bean WebServerFactoryCustomizer<TomcatServletWebServerFactory> recoveryConnector(
            @Value("${app.withdrawal-recovery.workload-port}") int port,
            @Value("${app.withdrawal-recovery.tls.certificate-file}") String certificate,
            @Value("${app.withdrawal-recovery.tls.private-key-file}") String privateKey) {
        if (port < 1 || port > 65535 || certificate.isBlank() || privateKey.isBlank())
            throw new IllegalArgumentException("Recovery HTTPS connector configuration required.");
        return factory -> {
            if (factory.getPort() == port || (factory.getContextPath() != null && !factory.getContextPath().isEmpty()))
                throw new IllegalArgumentException("Recovery connector must be isolated at the root context.");
            Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
            connector.setPort(port); connector.setScheme("https"); connector.setSecure(true);
            connector.setProperty("SSLEnabled", "true");
            var host = new SSLHostConfig(); host.setProtocols("TLSv1.2,TLSv1.3");
            var cert = new SSLHostConfigCertificate(host, SSLHostConfigCertificate.Type.RSA);
            cert.setCertificateFile(certificate); cert.setCertificateKeyFile(privateKey); host.addCertificate(cert);
            connector.addSslHostConfig(host);
            factory.addAdditionalTomcatConnectors(connector);
            factory.addEngineValves(new PortBoundary(port));
        };
    }

    static final class PortBoundary extends ValveBase {
        private final int port;
        PortBoundary(int port) { super(true); this.port = port; }
        static boolean allowed(int actual, int workload, String method, String path) {
            boolean internal = path.equals("/internal") || path.startsWith("/internal/");
            if (actual != workload) return !internal;
            return "POST".equals(method) && Arrays.stream(RecoveryProtocol.Route.values()).anyMatch(r -> r.path().equals(path));
        }
        @Override public void invoke(Request request, Response response) throws IOException, ServletException {
            int actual = request.getConnector().getPort();
            if (!allowed(actual, port, request.getMethod(), request.getDecodedRequestURI())) {
                response.setHeader("Cache-Control", "no-store"); response.setStatus(403); return;
            }
            request.setAttribute(CONNECTOR_PORT, actual);
            getNext().invoke(request, response);
        }
    }
}
