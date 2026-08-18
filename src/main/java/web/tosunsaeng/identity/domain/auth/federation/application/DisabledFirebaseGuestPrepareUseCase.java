package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestPrepareRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestPrepareResponse;

public final class DisabledFirebaseGuestPrepareUseCase implements FirebaseGuestPrepareUseCase {

	@Override
	public FirebaseGuestPrepareResponse prepare(FirebaseGuestPrepareRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
