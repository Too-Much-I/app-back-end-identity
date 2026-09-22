package web.tosunsaeng.identity.domain.auth.common.exception;

import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.observability.FailureDiagnostic;

public final class AuthException extends BusinessException {
	private final FailureDiagnostic diagnostic;

	public AuthException(AuthErrorStatus errorStatus) {
		this(errorStatus, null);
	}

	public AuthException(AuthErrorStatus errorStatus, FailureDiagnostic diagnostic) {
		super(errorStatus);
		this.diagnostic = diagnostic;
	}

	public FailureDiagnostic diagnostic() { return diagnostic; }
}
