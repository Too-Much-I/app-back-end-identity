package web.tosunsaeng.identity.domain.auth.common.api;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.auth.registration.dto.request.CheckEmailRequest;
import web.tosunsaeng.identity.domain.auth.registration.dto.request.GuestAuthRequest;
import web.tosunsaeng.identity.domain.auth.local.dto.request.LoginRequest;
import web.tosunsaeng.identity.domain.auth.session.dto.request.LogoutRequest;
import web.tosunsaeng.identity.domain.auth.session.dto.request.ReissueRequest;
import web.tosunsaeng.identity.domain.auth.registration.dto.request.SignupRequest;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.CheckEmailResponse;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.GuestAuthResponse;
import web.tosunsaeng.identity.domain.auth.local.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.SignupResponse;
import web.tosunsaeng.identity.domain.auth.registration.application.EmailAvailabilityService;
import web.tosunsaeng.identity.domain.auth.registration.application.GuestAuthService;
import web.tosunsaeng.identity.domain.auth.local.application.LoginService;
import web.tosunsaeng.identity.domain.auth.session.application.LogoutService;
import web.tosunsaeng.identity.domain.auth.session.application.LogoutAllService;
import web.tosunsaeng.identity.domain.auth.registration.application.SignupService;
import web.tosunsaeng.identity.domain.auth.session.application.TokenReissueService;
import web.tosunsaeng.identity.global.config.OpenApiConfig;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "Auth", description = "LOCAL·Guest 계정 인증과 로그인 세션 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final EmailAvailabilityService emailAvailabilityService;
	private final SignupService signupService;
	private final GuestAuthService guestAuthService;
	private final LoginService loginService;
	private final TokenReissueService tokenReissueService;
	private final LogoutService logoutService;
	private final LogoutAllService logoutAllService;

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
			description = "이메일 계정과 현재 개인정보 처리방침·이용약관 동의 상태를 생성합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "회원가입 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "입력값 검증, 필수 동의 누락 또는 현재 정책 버전 불일치",
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
			summary = "Guest 사용자 생성 및 인증",
			description = "설치 UUID를 중복 방지용으로만 사용해 ACTIVE Guest 사용자를 한 번 생성하고 "
					+ "서버의 현재 개인정보 처리방침과 이용약관 버전 동의 및 선택한 품질 검토 이용 동의를 저장한 뒤 "
					+ "기존 RS256 Access Token과 Opaque Refresh Token을 발급합니다. "
					+ "설치 UUID는 인증 수단이 아니므로 이미 생성된 Guest의 Token을 다시 발급하지 않습니다. "
					+ "응답 유실 또는 Token 분실 시 설치 UUID만으로 계정을 복구할 수 없습니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "Guest 사용자와 인증 토큰 발급 성공",
					useReturnTypeSchema = true,
					content = @Content(
							examples = @ExampleObject(value = """
									{
									  "isSuccess": true,
									  "code": "SUCCESS",
									  "message": "요청에 성공했습니다.",
									  "result": {
									    "accessToken": "<redacted>",
									    "refreshToken": "<redacted>",
									    "grantType": "Bearer",
									    "accessTokenExpiresIn": 1800000,
									    "refreshTokenExpiresIn": 1209600000
									  }
									}
									""")
					)
			),
			@ApiResponse(
					responseCode = "400",
					description = "설치 UUID 검증 실패, 필수 동의 누락 또는 동의한 정책의 현재 버전 불일치",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = BaseResponse.class),
							examples = {
									@ExampleObject(
											name = "PRIVACY_CONSENT_REQUIRED",
											value = """
													{
													  "isSuccess": false,
													  "code": "PRIVACY_CONSENT_REQUIRED",
													  "message": "개인정보 처리 동의가 필요합니다.",
													  "result": null
													}
													"""
									),
									@ExampleObject(
										name = "TERM_CONSENT_REQUIRED",
										value = """
												{
												  "isSuccess": false,
												  "code": "TERM_CONSENT_REQUIRED",
												  "message": "이용약관 동의가 필요합니다.",
												  "result": null
												}
											"""
									),
									@ExampleObject(
										name = "QUALITY_REVIEW_CONSENT_VERSION_MISMATCH",
										value = """
											{
											  "isSuccess": false,
											  "code": "QUALITY_REVIEW_CONSENT_VERSION_MISMATCH",
											  "message": "현재 품질 검토 이용 동의 버전과 일치하지 않습니다.",
											  "result": null
											}
											"""
									),
									@ExampleObject(
											name = "INVALID_REQUEST",
											value = """
													{
													  "isSuccess": false,
													  "code": "INVALID_REQUEST",
													  "message": "잘못된 요청입니다.",
													  "result": [{
													    "field": "installationId",
													    "rejectedValue": null,
													    "reason": "설치 ID는 UUID v4 형식이어야 합니다."
													  }]
													}
													"""
									)
							}
					)
			),
			@ApiResponse(
					responseCode = "409",
					description = "같은 설치 UUID로 Guest가 이미 생성됨",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = BaseResponse.class),
							examples = @ExampleObject(
									name = "GUEST_ALREADY_EXISTS",
									value = """
											{
											  "isSuccess": false,
											  "code": "GUEST_ALREADY_EXISTS",
											  "message": "이미 생성된 Guest 사용자입니다.",
											  "result": null
											}
											"""
						)
					)
			)
	})
	@PostMapping(
			value = "/guest",
			consumes = "application/json",
			produces = "application/json"
	)
	public BaseResponse<GuestAuthResponse> guest(
			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					description = "앱 설치 UUID, 현재 필수 개인정보 처리방침·이용약관 동의와 선택 품질 검토 이용 동의",
					content = @Content(
							schema = @Schema(implementation = GuestAuthRequest.class),
							examples = @ExampleObject(value = """
									{
									  "installationId": "550e8400-e29b-41d4-a716-446655440000",
									  "isPrivacyConsented": true,
									  "privacyConsentVersion": "privacy-v1",
									  "isTermConsented": true,
									  "termConsentVersion": "term-v1",
									  "isQualityReviewConsented": false,
									  "qualityReviewConsentVersion": "quality-review-v1"
									}
									""")
					)
			)
			@Valid @RequestBody GuestAuthRequest request
	) {
		return BaseResponse.success(guestAuthService.authenticate(request));
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
					description = "Refresh Token 거절: INVALID_REFRESH_TOKEN, "
							+ "REFRESH_TOKEN_EXPIRED, REFRESH_TOKEN_REUSE_DETECTED, "
							+ "ACCOUNT_WITHDRAWN",
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
