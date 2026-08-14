package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public final class DisabledFirebaseSignupUseCase implements FirebaseSignupUseCase {

	@Override
	public FirebaseSignupResponse signup(FirebaseSignupRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
