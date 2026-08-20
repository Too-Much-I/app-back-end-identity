package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public final class DisabledFirebaseGuestMergeUseCase implements FirebaseGuestMergeUseCase {

	@Override
	public FirebaseSignupResponse merge(FirebaseGuestMergeRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
