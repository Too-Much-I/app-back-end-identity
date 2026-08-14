package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseExchangeResponse;

public interface FirebaseExchangeUseCase {

	FirebaseExchangeResponse exchange(FirebaseExchangeRequest request);
}
