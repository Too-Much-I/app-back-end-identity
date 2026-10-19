package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Set;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

@Schema(description = "기존 MEMBER의 동기화된 소셜 인증수단")
public record FirebaseAuthMethodsSyncResponse(
		@Schema(description = "현재 Identity에 연결된 소셜 Provider")
		Set<SocialProvider> linkedProviders
) {

	public FirebaseAuthMethodsSyncResponse {
		linkedProviders = Set.copyOf(Objects.requireNonNull(
				linkedProviders,
				"linkedProviders must not be null"
		));
	}
}
