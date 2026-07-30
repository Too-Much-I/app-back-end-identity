package web.tosunsaeng.identity.domain.user.exception;

import web.tosunsaeng.identity.global.exception.BusinessException;

public final class UserException extends BusinessException {

	public UserException(UserErrorStatus errorStatus) {
		super(errorStatus);
	}
}
