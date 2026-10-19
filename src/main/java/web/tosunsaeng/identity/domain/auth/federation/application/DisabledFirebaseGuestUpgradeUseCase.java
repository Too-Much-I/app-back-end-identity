package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestUpgradeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public final class DisabledFirebaseGuestUpgradeUseCase implements FirebaseGuestUpgradeUseCase {

	@Override
	public FirebaseSignupResponse upgrade(FirebaseGuestUpgradeRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
