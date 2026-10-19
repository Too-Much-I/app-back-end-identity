package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import jakarta.servlet.http.HttpServletRequest;

/** Workload credential boundary. Bearer JWT does not sign the request body or headers.
 * Owner comes only from trusted service configuration, never from forwarding headers or user JWT. */
public interface RecoveryIngressVerifier {
    RecoveryCursor.Owner authorizeHeaders(HttpServletRequest request, RecoveryProtocol.Route route);

    final class Unauthenticated extends SecurityException {
        public Unauthenticated() { super("Workload authentication rejected."); }
    }
}
