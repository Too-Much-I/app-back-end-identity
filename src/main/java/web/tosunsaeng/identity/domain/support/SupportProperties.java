package web.tosunsaeng.identity.domain.support;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.support")
public record SupportProperties(boolean enabled, boolean workerEnabled, String hmacKey,
        String slackWebhookUrl, List<String> trustedProxies) {
    public SupportProperties {
        trustedProxies = trustedProxies == null ? List.of() : List.copyOf(trustedProxies);
    }
    @Override public String toString() { return "SupportProperties[secrets=REDACTED]"; }
}
