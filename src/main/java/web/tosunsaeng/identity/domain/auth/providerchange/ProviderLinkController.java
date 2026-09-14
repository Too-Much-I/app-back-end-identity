package web.tosunsaeng.identity.domain.auth.providerchange;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.global.response.BaseResponse;
import web.tosunsaeng.identity.global.security.currentuser.CurrentUserProvider;

@RestController
@RequestMapping("/api/v1/auth/firebase/providers/link")
@SecurityRequirement(name = "bearerAuth")
public class ProviderLinkController {
	public record AttemptRequest(@NotBlank @Size(max = 36) String linkAttemptId,
			@Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") @NotBlank @Size(max = 16384) String firebaseIdToken) {
		@Override public String toString() { return "ProviderLinkRequest[REDACTED]"; }
	}
	private final ObjectProvider<ProviderLinkService> service;
	private final CurrentUserProvider currentUser;
	public ProviderLinkController(ObjectProvider<ProviderLinkService> service, CurrentUserProvider currentUser) {
		this.service = service; this.currentUser = currentUser;
	}
	@Operation(summary = "SNS 공통 연결 준비", description = "최초/재연결 구분 없이 기존 SNS로 재인증합니다. 준비만으로 Firebase link를 실행하면 안 됩니다.")
	@PostMapping(value = "/prepare", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> prepare(@Valid @RequestBody ProviderChangeController.ChangeRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) List<String> keys) {
		return BaseResponse.success(required().prepare(currentUser.getCurrentUserId(), request.provider(), request.firebaseIdToken(), keys));
	}
	@Operation(summary = "SNS 연결 시작", description = "linkAllowed=true인 최초 응답에만 Firebase link 1회를 허용합니다. 재요청은 허가를 다시 발급하지 않습니다.")
	@PostMapping(value = "/start", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> start(@Valid @RequestBody AttemptRequest request) {
		return BaseResponse.success(required().start(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken()));
	}
	@Operation(summary = "SNS 공통 연결 완료", description = "동일 Firebase User에서 대상 SNS 재인증 후 갱신한 Token이 필요합니다. 대상 SNS만 저장합니다.")
	@PostMapping(value = "/complete", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> complete(@Valid @RequestBody AttemptRequest request) {
		return BaseResponse.success(required().complete(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken()));
	}
	@Operation(summary = "SNS 연결 상태 조회", description = "준비 당시 requestId와 Firebase 본인 증거 및 사용자 JWT로 조회합니다. 새로운 Firebase link를 허가하지 않습니다.")
	@PostMapping(value = "/status", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> status(@Valid @RequestBody ProviderChangeController.StatusRequest request) {
		return BaseResponse.success(required().status(currentUser.getCurrentUserId(), request.requestId(), request.firebaseIdToken()));
	}
	private ProviderLinkService required() {
		var value = service.getIfAvailable();
		if (value == null) throw ProviderChangeGuard.error(AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
		return value;
	}
}
