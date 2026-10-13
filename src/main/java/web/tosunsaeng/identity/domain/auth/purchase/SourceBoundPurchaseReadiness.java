package web.tosunsaeng.identity.domain.auth.purchase;

import java.util.Objects;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.CaptureReadiness;

/** Local source/config checks only. Migration, writer drain and clock health remain deployment responsibilities. */
public final class SourceBoundPurchaseReadiness implements PurchaseReadiness {
    private final CaptureReadiness capture;
    private final PurchaseProperties purchase;
    private final boolean sessionFence;
    public SourceBoundPurchaseReadiness(CaptureReadiness capture, PurchaseProperties purchase, boolean sessionFence) {
        this.capture=Objects.requireNonNull(capture);
        this.purchase=Objects.requireNonNull(purchase);
        this.sessionFence=sessionFence;
    }
    @Override public boolean ready() {
        // Scope OFF must not bypass readiness for an already-issued purchase response replay.
        if (!purchase.isControlWritesEnabled() || !purchase.isFenceEnabled() || !sessionFence
                || !purchase.hasValidSourceBinding()) return false;
        String expected=purchase.getSourceStreamId();
        return capture.activeSource().map(source -> expected.equals(source.streamId())).orElse(false);
    }
}
