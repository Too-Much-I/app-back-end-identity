package web.tosunsaeng.identity.domain.auth.accountrecovery;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.global.response.BaseResponse;

@RestController
@RequestMapping("/api/v1/auth/account-recovery")
public class AccountRecoveryController {
	public record LookupRequest(@NotBlank @Size(max=36) String recoveryId,
			@NotBlank @Size(max=16384) @Schema(accessMode=Schema.AccessMode.WRITE_ONLY, format="password") String firebaseIdToken) {
		@Override public String toString() { return "RecoveryLookupRequest[REDACTED]"; }
	}
	private final ObjectProvider<AccountRecoveryService> services;
	public AccountRecoveryController(ObjectProvider<AccountRecoveryService> services) { this.services=services; }
	@Operation(summary="계정 찾기 준비", description="Identity 로그인 없이 접수번호를 발급합니다. 이후 SMS 인증을 진행하세요. 회원 존재 여부를 반환하지 않습니다.")
	@PostMapping(value="/prepare", produces="application/json")
	public BaseResponse<AccountRecoveryService.Prepared> prepare(HttpServletRequest request) {
		return BaseResponse.success(service().prepare(request.getRemoteAddr()));
	}
	@Operation(summary="SMS 인증 후 가입 SNS 안내", description="최근 PHONE 인증과 접수번호가 필요합니다. SNS/마스킹 이메일 힌트만 반환하며 회원 로그인·병합은 수행하지 않습니다. 같은 접수번호/전화 인증으로 응답 유실 재시도가 가능합니다.")
	@PostMapping(value="/lookup", consumes="application/json", produces="application/json")
	public BaseResponse<RecoveryResult> lookup(@Valid @RequestBody LookupRequest body, HttpServletRequest request) {
		return BaseResponse.success(service().lookup(body.recoveryId(), body.firebaseIdToken(), request.getRemoteAddr()));
	}
	@ExceptionHandler(AuthException.class)
	public ResponseEntity<BaseResponse<Void>> failure(AuthException e) {
		var builder = ResponseEntity.status(e.getErrorCode().getHttpStatus()).header("Cache-Control", "no-store");
		return builder.body(BaseResponse.failure(e.getErrorCode()));
	}
	@ExceptionHandler(RecoveryRateLimitException.class)
	public ResponseEntity<BaseResponse<Void>> rateLimited(RecoveryRateLimitException e) {
		return ResponseEntity.status(e.getErrorCode().getHttpStatus())
				.header("Cache-Control", "no-store")
				.header("Retry-After", Long.toString(e.retryAfterSeconds()))
				.body(BaseResponse.failure(e.getErrorCode()));
	}
	private AccountRecoveryService service() {
		var service = services.getIfAvailable();
		if (service == null) throw new AuthException(AuthErrorStatus.RECOVERY_UNAVAILABLE);
		return service;
	}
}
