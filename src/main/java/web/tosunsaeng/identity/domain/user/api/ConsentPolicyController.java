package web.tosunsaeng.identity.domain.user.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;
import web.tosunsaeng.identity.domain.user.dto.response.CurrentConsentPolicyResponse;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "Consent Policy", description = "가입 전 공개 정책 버전 조회")
@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class ConsentPolicyController {

	private final ConsentPolicy consentPolicy;

	@Operation(summary = "현재 정책 버전 공개 조회",
			description = "Identity 인증 없이 현재 필수·선택 정책 버전을 조회합니다. "
					+ "사용자가 확인한 버전을 가입 요청에 보내야 하며 제출 시 현재 버전을 다시 검증합니다.")
	@GetMapping(value = "/consents", produces = "application/json")
	public ResponseEntity<BaseResponse<CurrentConsentPolicyResponse>> getCurrentPolicies() {
		return ResponseEntity.ok().cacheControl(CacheControl.noStore())
				.body(BaseResponse.success(CurrentConsentPolicyResponse.from(consentPolicy)));
	}
}
