package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseAuthMethodsSyncRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthMethodsSyncResponse;

public final class DisabledFirebaseAuthMethodsSyncUseCase implements FirebaseAuthMethodsSyncUseCase {

	@Override
	public FirebaseAuthMethodsSyncResponse sync(FirebaseAuthMethodsSyncRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
