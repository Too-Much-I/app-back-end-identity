package web.tosunsaeng.identity.domain.auth.api;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.auth.dto.request.CheckEmailRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.LogoutRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.application.EmailAvailabilityService;
import web.tosunsaeng.identity.domain.auth.application.LoginService;
import web.tosunsaeng.identity.domain.auth.application.LogoutService;
import web.tosunsaeng.identity.domain.auth.application.LogoutAllService;
import web.tosunsaeng.identity.domain.auth.application.SignupService;
import web.tosunsaeng.identity.domain.auth.application.TokenReissueService;
import web.tosunsaeng.identity.global.config.OpenApiConfig;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "Auth", description = "이메일 계정 인증과 로그인 세션 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final EmailAvailabilityService emailAvailabilityService;
	private final SignupService signupService;
	private final LoginService loginService;
	private final TokenReissueService tokenReissueService;
	private final LogoutService logoutService;
	private final LogoutAllService logoutAllService;

	public AuthController(
			EmailAvailabilityService emailAvailabilityService,
			SignupService signupService,
			LoginService loginService,
			TokenReissueService tokenReissueService,
			LogoutService logoutService,
			LogoutAllService logoutAllService
	) {
		this.emailAvailabilityService = emailAvailabilityService;
		this.signupService = signupService;
		this.loginService = loginService;
		this.tokenReissueService = tokenReissueService;
		this.logoutService = logoutService;
		this.logoutAllService = logoutAllService;
	}

	@Operation(
			summary = "이메일 중복 확인",
			description = "정규화한 이메일을 기준으로 회원가입 가능 여부를 확인합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "이메일 사용 가능 여부 확인 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/check-email")
	public BaseResponse<CheckEmailResponse> checkEmail(
			@Valid @RequestBody CheckEmailRequest request
	) {
		return BaseResponse.success(emailAvailabilityService.checkEmail(request.email()));
	}

	@Operation(
			summary = "일반 이메일 회원가입",
			description = "이메일 계정과 음성 데이터 수집·이용 동의 상태를 생성합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "회원가입 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증 또는 음성 데이터 동의 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "409",
					description = "이미 사용 중인 이메일",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/signup")
	public BaseResponse<SignupResponse> signup(
			@Valid @RequestBody SignupRequest request
	) {
		return BaseResponse.success(signupService.signup(request));
	}

	@Operation(
			summary = "일반 이메일 로그인",
			description = "이메일 계정의 자격증명과 상태를 확인하고 인증 토큰을 발급합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그인 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "이메일 또는 비밀번호 불일치",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "활성 상태가 아닌 계정",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/login")
	public BaseResponse<LoginResponse> login(
			@Valid @RequestBody LoginRequest request
	) {
		return BaseResponse.success(loginService.login(request));
	}

	@Operation(
			summary = "인증 토큰 재발급",
			description = "Refresh Token을 Rotation하고 새 Access Token과 Refresh Token을 발급합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "인증 토큰 재발급 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "401",
					description = "유효하지 않거나 만료·재사용된 Refresh Token",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "활성 상태가 아닌 계정",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/reissue")
	public BaseResponse<ReissueResponse> reissue(
			@Valid @RequestBody ReissueRequest request
	) {
		return BaseResponse.success(tokenReissueService.reissue(request));
	}

	@Operation(
			summary = "로그아웃",
			description = "RefreshSession을 멱등적으로 폐기합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "로그아웃 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/logout")
	public BaseResponse<Void> logout(
			@Valid @RequestBody LogoutRequest request
	) {
		logoutService.logout(request);
		return BaseResponse.success(null);
	}

	@Operation(
			summary = "전체 로그아웃",
			description = "현재 사용자의 활성 RefreshSession을 모두 멱등적으로 폐기합니다."
	)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "전체 로그아웃 성공"),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "권한 부족",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PostMapping("/logout-all")
	public BaseResponse<Void> logoutAll() {
		logoutAllService.logoutAll();
		return BaseResponse.success(null);
	}
}
