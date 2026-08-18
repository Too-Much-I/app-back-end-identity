package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestPrepareRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseGuestPrepareResponse;

public interface FirebaseGuestPrepareUseCase {

	FirebaseGuestPrepareResponse prepare(FirebaseGuestPrepareRequest request);
}
