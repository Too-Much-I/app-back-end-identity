package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseExchangeResponse;

public final class DisabledFirebaseExchangeUseCase implements FirebaseExchangeUseCase {

	@Override
	public FirebaseExchangeResponse exchange(FirebaseExchangeRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
