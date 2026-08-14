package web.tosunsaeng.identity.domain.auth.common.exception;

import web.tosunsaeng.identity.global.exception.BusinessException;

public final class AuthException extends BusinessException {

	public AuthException(AuthErrorStatus errorStatus) {
		super(errorStatus);
	}
}
