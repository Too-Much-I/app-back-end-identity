package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
		description = "Firebase 교환 결과",
		oneOf = {
				FirebaseAuthenticatedResponse.class,
				FirebaseEnrollmentRequiredResponse.class
		}
)
public sealed interface FirebaseExchangeResponse permits
		FirebaseAuthenticatedResponse,
		FirebaseEnrollmentRequiredResponse {

	FirebaseExchangeResultType type();
}
