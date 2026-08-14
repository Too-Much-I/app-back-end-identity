package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;

public interface FirebaseSignupUseCase {

	FirebaseSignupResponse signup(FirebaseSignupRequest request);
}
