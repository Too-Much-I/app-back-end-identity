package web.tosunsaeng.identity.domain.user.withdrawalcapture;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.withdrawal-capture")
public class WithdrawalCaptureProperties {
    private boolean required, publisherEnabled, cleanupEnabled;
    private String environment;
    private URI billingOrigin;
    private String region;
    public boolean isRequired() { return required; }
    public void setRequired(boolean v) { required=v; }
    public boolean isPublisherEnabled() { return publisherEnabled; }
    public void setPublisherEnabled(boolean v) { publisherEnabled=v; }
    public boolean isCleanupEnabled() { return cleanupEnabled; }
    public void setCleanupEnabled(boolean v) { cleanupEnabled=v; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String v) { environment=v; }
    public URI getBillingOrigin() { return billingOrigin; }
    public void setBillingOrigin(URI v) { billingOrigin=v; }
    public String getRegion() { return region; }
    public void setRegion(String v) { region=v; }
    public void validate(boolean controlWrites, boolean sessionFence) {
        if (required && (environment == null || !environment.matches("[a-z0-9-]{1,40}")))
            throw new IllegalStateException("Withdrawal environment required.");
        if (required && (!controlWrites || !sessionFence))
            throw new IllegalStateException("Capture requires compatible control writers and session fence.");
        if ((publisherEnabled || cleanupEnabled) && !required)
            throw new IllegalStateException("Delivery/cleanup require capture.");
        if (publisherEnabled && (billingOrigin == null || region == null || region.isBlank()))
            throw new IllegalStateException("Billing transport configuration missing.");
    }
}
