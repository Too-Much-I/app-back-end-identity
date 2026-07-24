package web.tosunsaeng.identity.auth.controller;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.auth.dto.request.CheckEmailRequest;
import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.service.AuthService;
import web.tosunsaeng.identity.common.response.BaseResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@Operation(
			summary = "이메일 중복 확인",
			description = "정규화한 이메일을 기준으로 회원가입 가능 여부를 확인합니다."
	)
	@PostMapping("/check-email")
	public BaseResponse<CheckEmailResponse> checkEmail(
			@Valid @RequestBody CheckEmailRequest request
	) {
		return BaseResponse.success(authService.checkEmail(request.email()));
	}

	@Operation(
			summary = "일반 이메일 회원가입",
			description = "이메일 계정과 음성 데이터 수집·이용 동의 상태를 생성합니다."
	)
	@PostMapping("/signup")
	public BaseResponse<SignupResponse> signup(
			@Valid @RequestBody SignupRequest request
	) {
		return BaseResponse.success(authService.signup(request));
	}
}
