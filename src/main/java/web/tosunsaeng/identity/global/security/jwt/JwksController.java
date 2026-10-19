package web.tosunsaeng.identity.global.security.jwt;

import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "JWKS", description = "Access Token 서명 검증용 공개 키 API")
@RestController
@RequiredArgsConstructor
public class JwksController {

	private final JwksPublicKeySet publicKeySet;

	@GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(
			summary = "JWKS 조회",
		description = "Identity Service의 현재·rotation overlap Public JWK를 표준 JWKS 형식으로 반환합니다."
	)
	@ApiResponse(responseCode = "200", description = "JWKS 조회 성공")
	public Map<String, Object> jwks() {
		// 서명 검증에 필요한 공개 JWK만 외부에 제공한다.
		return publicKeySet.toJsonObject();
	}
}
