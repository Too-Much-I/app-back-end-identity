package web.tosunsaeng.identity.domain.auth.providerchange;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
	public record ProofRequest(@Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password")
			@NotBlank @Size(max = 16384) String firebaseIdToken) {
		@Override public String toString() { return "ProviderLinkProof[REDACTED]"; }
	}
	public record FailureRequest(@NotBlank @Size(max = 36) String linkAttemptId,
			@Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") @NotBlank @Size(max = 16384) String firebaseIdToken,
			@NotNull ProviderLinkAttempt.FailureCode failureCode) {
		@Override public String toString() { return "ProviderLinkFailure[REDACTED]"; }
	}
	private final CurrentUserProvider currentUser;
	public ProviderLinkController(ObjectProvider<ProviderLinkService> service, CurrentUserProvider currentUser) {
		this.service = service; this.currentUser = currentUser;
	}
	@Operation(summary = "[폐지] SNS 공통 연결 준비", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/prepare", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> prepare(@Valid @RequestBody ProviderChangeController.ChangeRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) List<String> keys) {
		return BaseResponse.success(required().prepare(currentUser.getCurrentUserId(), request.provider(), request.firebaseIdToken(), keys));
	}
	@Operation(summary = "[폐지] SNS 연결 시작", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/start", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> start(@Valid @RequestBody AttemptRequest request) {
		return BaseResponse.success(required().start(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken()));
	}
	@Operation(summary = "[폐지] SNS 공통 연결 완료", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/complete", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> complete(@Valid @RequestBody AttemptRequest request) {
		return BaseResponse.success(required().complete(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken()));
	}
	@Operation(summary = "[폐지] SNS 연결 상태 조회", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/status", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> status(@Valid @RequestBody ProviderChangeController.StatusRequest request) {
		return BaseResponse.success(required().status(currentUser.getCurrentUserId(), request.requestId(), request.firebaseIdToken()));
	}
	private ProviderLinkService required() {
		throw ProviderChangeGuard.error(AuthErrorStatus.PROVIDER_LINK_RETIRED);
	}
	@Operation(summary = "[폐지] SNS 연결 취소 요청", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/cancel", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> cancel(@Valid @RequestBody AttemptRequest request) {
		return BaseResponse.success(required().cancel(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken()));
	}
	@Operation(summary = "[폐지] SNS 연결 실패 보고", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/failure-report", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Status> failure(@Valid @RequestBody FailureRequest request) {
		return BaseResponse.success(required().failure(currentUser.getCurrentUserId(), request.linkAttemptId(), request.firebaseIdToken(), request.failureCode()));
	}
	@Operation(summary = "[폐지] 본인 진행 중 SNS 연결 조회", deprecated = true, description = "단일 SNS 정책으로 폐지. 유효 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.")
	@PostMapping(value = "/pending", consumes = "application/json", produces = "application/json")
	public BaseResponse<ProviderLinkService.Pending> pending(@Valid @RequestBody ProofRequest request) {
		return BaseResponse.success(required().pending(currentUser.getCurrentUserId(), request.firebaseIdToken()));
	}
}
