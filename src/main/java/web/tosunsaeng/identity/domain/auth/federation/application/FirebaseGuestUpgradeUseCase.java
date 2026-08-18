package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseGuestUpgradeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public interface FirebaseGuestUpgradeUseCase {

	FirebaseSignupResponse upgrade(FirebaseGuestUpgradeRequest request);
}
