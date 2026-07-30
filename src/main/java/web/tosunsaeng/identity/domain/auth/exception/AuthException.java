package web.tosunsaeng.identity.domain.auth.exception;

import web.tosunsaeng.identity.global.exception.BusinessException;

public final class AuthException extends BusinessException {

	public AuthException(AuthErrorStatus errorStatus) {
		super(errorStatus);
	}
}
