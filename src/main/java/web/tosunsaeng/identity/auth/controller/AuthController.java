package web.tosunsaeng.identity.auth.controller;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.auth.dto.request.CheckEmailRequest;
import web.tosunsaeng.identity.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.auth.dto.request.LogoutRequest;
import web.tosunsaeng.identity.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.auth.service.AuthService;
import web.tosunsaeng.identity.auth.service.LogoutAllService;
import web.tosunsaeng.identity.common.response.BaseResponse;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;
	private final LogoutAllService logoutAllService;

	public AuthController(AuthService authService, LogoutAllService logoutAllService) {
		this.authService = authService;
		this.logoutAllService = logoutAllService;
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

	@Operation(
			summary = "일반 이메일 로그인",
			description = "이메일 계정의 자격증명과 상태를 확인하고 인증 토큰을 발급합니다."
	)
	@PostMapping("/login")
	public BaseResponse<LoginResponse> login(
			@Valid @RequestBody LoginRequest request
	) {
		return BaseResponse.success(authService.login(request));
	}

	@Operation(
			summary = "인증 토큰 재발급",
			description = "Refresh Token을 Rotation하고 새 Access Token과 Refresh Token을 발급합니다."
	)
	@PostMapping("/reissue")
	public BaseResponse<ReissueResponse> reissue(
			@Valid @RequestBody ReissueRequest request
	) {
		return BaseResponse.success(authService.reissue(request));
	}

	@Operation(
			summary = "로그아웃",
			description = "RefreshSession을 멱등적으로 폐기합니다."
	)
	@PostMapping("/logout")
	public BaseResponse<Void> logout(
			@Valid @RequestBody LogoutRequest request
	) {
		authService.logout(request);
		return BaseResponse.success(null);
	}

	@Operation(
			summary = "전체 로그아웃",
			description = "현재 사용자의 활성 RefreshSession을 모두 멱등적으로 폐기합니다."
	)
	@PostMapping("/logout-all")
	public BaseResponse<Void> logoutAll() {
		logoutAllService.logoutAll();
		return BaseResponse.success(null);
	}
}
