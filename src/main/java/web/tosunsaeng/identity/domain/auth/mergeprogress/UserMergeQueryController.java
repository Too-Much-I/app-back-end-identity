package web.tosunsaeng.identity.domain.auth.mergeprogress;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import web.tosunsaeng.identity.global.response.BaseResponse;
import web.tosunsaeng.identity.global.config.OpenApiConfig;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/merges")
@Tag(name="Merge Progress", description="현재 ACTIVE MEMBER 본인의 Guest 병합 이전 상태")
@SecurityRequirement(name=OpenApiConfig.BEARER_AUTH)
public class UserMergeQueryController {
	private final UserMergeQueryService service;
	@GetMapping("/{mergeId}")
	@Operation(summary="내 병합 작업 조회", description="완료는 생성 당시 필수 consumer의 commit 후 204 확인을 의미합니다.")
	public BaseResponse<MergeProgressResponse> get(@PathVariable String mergeId,
			@AuthenticationPrincipal Jwt jwt) {
		requireMember(jwt);
		return BaseResponse.success(service.get(mergeId));
	}
	@GetMapping
	@Operation(summary="내 병합 작업 목록", description="응답 유실 시 회원 exchange 후 조회. activeOnly=false로 완료 기록 포함. cursor 유효시간 1시간.")
	public BaseResponse<MergeProgressResponse.Page> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue="true") String activeOnly,
			@RequestParam(defaultValue="20") String limit,
			@RequestParam(required=false) String cursor) {
		requireMember(jwt);
		return BaseResponse.success(service.list(activeOnly, limit, cursor));
	}
	private void requireMember(Jwt jwt) {
		if (jwt == null || !"MEMBER".equals(jwt.getClaimAsString("account_type")))
			throw new UserException(
					UserErrorStatus.ACCOUNT_NOT_ACTIVE);
	}
	@ExceptionHandler(MergeQuerySupport.RateLimited.class)
	public ResponseEntity<BaseResponse<Void>> limited(MergeQuerySupport.RateLimited e) {
		return ResponseEntity.status(429).header("Retry-After", Long.toString(e.retryAfterSeconds()))
				.body(BaseResponse.failure(AuthErrorStatus.MERGE_STATUS_RATE_LIMITED));
	}
}
