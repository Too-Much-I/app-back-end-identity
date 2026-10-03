package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestMergeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestMergeResponse;

public interface FirebaseGuestMergeUseCase {

	FirebaseGuestMergeResponse merge(FirebaseGuestMergeRequest request);
}
