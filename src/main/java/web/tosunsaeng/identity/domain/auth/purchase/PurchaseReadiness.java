package web.tosunsaeng.identity.domain.auth.purchase;

/** Implemented only when control migration, writer drain and I2 durable capture are verified.
 * A deployment flag alone is not evidence. No permissive default implementation is supplied. */
public interface PurchaseReadiness {
    boolean ready();
}
