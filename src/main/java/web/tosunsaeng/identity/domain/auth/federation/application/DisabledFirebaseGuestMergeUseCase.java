package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestMergeResponse;

public final class DisabledFirebaseGuestMergeUseCase implements FirebaseGuestMergeUseCase {

	@Override
	public FirebaseGuestMergeResponse merge(FirebaseGuestMergeRequest request) {
		throw new AuthException(AuthErrorStatus.FIREBASE_UNAVAILABLE);
	}
}
