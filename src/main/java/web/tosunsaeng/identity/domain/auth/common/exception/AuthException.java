package web.tosunsaeng.identity.domain.auth.common.exception;

import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.observability.FailureDiagnostic;

public final class AuthException extends BusinessException {
	private final FailureDiagnostic diagnostic;
	private final SessionRejectionReason sessionRejectionReason;

	public static AuthException loggedOut(SessionRejectionReason reason) {
		return new AuthException(AuthErrorStatus.SESSION_LOGGED_OUT, null, reason);
	}

	public SessionRejectionReason sessionRejectionReason() { return sessionRejectionReason; }

	public AuthException(AuthErrorStatus errorStatus) {
		this(errorStatus, null);
	}

	public AuthException(AuthErrorStatus errorStatus, FailureDiagnostic diagnostic) {
		this(errorStatus, diagnostic, SessionRejectionReason.UNKNOWN);
	}

	private AuthException(AuthErrorStatus errorStatus, FailureDiagnostic diagnostic, SessionRejectionReason reason) {
		super(errorStatus);
		this.diagnostic = diagnostic;
		this.sessionRejectionReason = java.util.Objects.requireNonNull(reason);
	}

	public FailureDiagnostic diagnostic() { return diagnostic; }
}
