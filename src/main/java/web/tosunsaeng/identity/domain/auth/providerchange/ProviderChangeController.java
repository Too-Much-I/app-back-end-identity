package web.tosunsaeng.identity.domain.auth.providerchange;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.global.response.BaseResponse;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

@RestController
@RequestMapping("/api/v1/auth/firebase/providers")
public class ProviderChangeController {
	public record ChangeRequest(@NotNull SocialProvider provider,
			@Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") @NotBlank @Size(max = 16384) String firebaseIdToken) {
		@Override public String toString() { return "ProviderChangeRequest[provider=" + provider + ",credential=REDACTED]"; }
	}
	public record StatusRequest(@NotBlank @Size(max = 36) String requestId,
			@Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") @NotBlank @Size(max = 16384) String firebaseIdToken) {
		@Override public String toString() { return "ProviderStatusRequest[REDACTED]"; }
	}
	private final ObjectProvider<ProviderChangeService> service;
	private final CurrentUserProvider currentUser;
	public ProviderChangeController(ObjectProvider<ProviderChangeService> service, CurrentUserProvider currentUser) {
		this.service = service; this.currentUser = currentUser;
	}
	@Operation(summary = "[폐지] SNS 연결 해제 접수", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "410", description = "PROVIDER_LINK_RETIRED")
	@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = "/unlink", consumes = "application/json", produces = "application/json")
	public ResponseEntity<BaseResponse<ProviderChangeService.Status>> unlink(@Valid @RequestBody ChangeRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) List<String> keys) {
		return ResponseEntity.accepted().body(BaseResponse.success(required().unlink(currentUser.getCurrentUserId(), request.provider(), request.firebaseIdToken(), keys)));
	}
	@Operation(summary = "[폐지] SNS 연결 해제 상태 조회", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/unlink/status", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderChangeService.Status> status(@Valid @RequestBody StatusRequest request) {
		return BaseResponse.success(required().status(request.requestId(), request.firebaseIdToken()));
	}
	private ProviderChangeService required() {
		throw ProviderChangeGuard.error(AuthErrorStatus.PROVIDER_LINK_RETIRED);
	}
}
