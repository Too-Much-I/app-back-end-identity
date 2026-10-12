package web.tosunsaeng.identity.domain.auth.purchase;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Flags are intentionally independent: reader/writer preparation does not grant purchase. */
@ConfigurationProperties("app.purchase")
public class PurchaseProperties {
    private boolean fenceEnabled;
    private boolean controlWritesEnabled;
    private boolean scopeEnabled;
    public boolean isFenceEnabled() { return fenceEnabled; }
    public void setFenceEnabled(boolean value) { fenceEnabled = value; }
    public boolean isControlWritesEnabled() { return controlWritesEnabled; }
    public void setControlWritesEnabled(boolean value) { controlWritesEnabled = value; }
    public boolean isScopeEnabled() { return scopeEnabled; }
    public void setScopeEnabled(boolean value) { scopeEnabled = value; }
    public void validate(Duration ttl, boolean sessionFence, boolean readiness) {
        if (fenceEnabled && (!controlWritesEnabled || !sessionFence))
            throw new IllegalStateException("Purchase fence requires compatible control writers and session protection.");
        if (scopeEnabled && (!fenceEnabled || !sessionFence || !readiness))
            throw new IllegalStateException("Purchase requires issuance, session and capture readiness.");
        if (scopeEnabled && (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofMinutes(30)) > 0))
            throw new IllegalStateException("Purchase token TTL must be positive and at most 30 minutes.");
    }
}
