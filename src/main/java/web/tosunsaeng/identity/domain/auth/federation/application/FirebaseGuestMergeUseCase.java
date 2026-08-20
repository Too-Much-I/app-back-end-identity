package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public interface FirebaseGuestMergeUseCase {

	FirebaseSignupResponse merge(FirebaseGuestMergeRequest request);
}
