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
	@Operation(summary = "SNS 연결 해제 접수", description = "남는 수단의 최근 Firebase 인증 및 사용자 JWT가 필요합니다. 202는 접수이며 모든 자체 세션을 종료합니다.")
	@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = "/unlink", consumes = "application/json", produces = "application/json")
	public ResponseEntity<BaseResponse<ProviderChangeService.Status>> unlink(@Valid @RequestBody ChangeRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) List<String> keys) {
		return ResponseEntity.accepted().body(BaseResponse.success(required().unlink(currentUser.getCurrentUserId(), request.provider(), request.firebaseIdToken(), keys)));
	}
	@Operation(summary = "SNS 연결 해제 상태 조회", description = "남는 SNS의 Firebase 증거로 본인 확인합니다. 사용자 JWT 없이 조회하지만 요청 ID만으로 조회할 수 없습니다. Token을 발급하지 않습니다.")
	@PostMapping(value = "/unlink/status", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderChangeService.Status> status(@Valid @RequestBody StatusRequest request) {
		return BaseResponse.success(required().status(request.requestId(), request.firebaseIdToken()));
	}
	@Operation(summary = "폐기된 재연결 준비", deprecated = true, description = "항상 503을 반환합니다. 공통 providers/link/prepare → start → complete로 전환해야 합니다.")
	@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = "/relink/prepare", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderChangeService.Permit> prepare(@Valid @RequestBody ChangeRequest request) {
		return BaseResponse.success(required().prepare(currentUser.getCurrentUserId(), request.provider(), request.firebaseIdToken()));
	}
	private ProviderChangeService required() {
		var value = service.getIfAvailable();
		if (value == null) throw ProviderChangeGuard.error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		return value;
	}
}
