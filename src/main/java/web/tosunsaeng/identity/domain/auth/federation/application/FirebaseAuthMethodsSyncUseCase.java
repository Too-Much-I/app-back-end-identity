package web.tosunsaeng.identity.domain.auth.federation.application;

import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseAuthMethodsSyncRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseAuthMethodsSyncResponse;

public interface FirebaseAuthMethodsSyncUseCase {

	FirebaseAuthMethodsSyncResponse sync(FirebaseAuthMethodsSyncRequest request);
}
