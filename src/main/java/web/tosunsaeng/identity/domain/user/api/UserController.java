package web.tosunsaeng.identity.domain.user.api;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.user.application.UserConsentService;
import web.tosunsaeng.identity.domain.user.application.UserProfileService;
import web.tosunsaeng.identity.domain.user.dto.request.UserConsentUpdateRequest;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserConsentStatusResponse;
import web.tosunsaeng.identity.domain.user.dto.response.UserProfileResponse;
import web.tosunsaeng.identity.global.config.OpenApiConfig;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "User", description = "인증된 사용자 프로필 및 정책 동의 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

	private final UserProfileService userProfileService;
	private final UserConsentService userConsentService;

	@Operation(
			summary = "내 프로필 조회",
			description = "검증된 Access Token의 subject에 해당하는 사용자 프로필을 조회합니다."
	)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "내 프로필 조회 성공"),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "활성 상태가 아닌 계정 또는 권한 부족",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "사용자를 찾을 수 없음",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@GetMapping("/me")
	public BaseResponse<UserProfileResponse> getMe() {
		return BaseResponse.success(userProfileService.getCurrentUserProfile());
	}

	@Operation(
			summary = "개인정보 처리방침 및 이용약관 동의 상태 조회",
			description = "검증된 Access Token의 subject 사용자가 저장한 동의 버전과 "
					+ "서버의 현재 필수 버전을 비교해 신규 동의 필요 여부를 반환합니다."
	)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "동의 상태 조회 성공"),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "활성 상태가 아닌 계정",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "사용자를 찾을 수 없음",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@GetMapping(value = "/me/consents", produces = "application/json")
	public BaseResponse<UserConsentStatusResponse> getConsents() {
		return BaseResponse.success(userConsentService.getCurrentConsentStatus());
	}

	@Operation(
			summary = "개인정보 처리방침 및 이용약관 동의 갱신",
			description = "검증된 Access Token의 subject 사용자가 서버의 현재 필수 정책 버전에 "
					+ "동의한 상태를 저장합니다. 동일 버전 재요청은 기존 동의 시각을 유지합니다."
	)
	@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "동의 갱신 또는 멱등 확인 성공"),
			@ApiResponse(
					responseCode = "400",
					description = "필수 동의 누락 또는 현재 정책 버전 불일치",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = BaseResponse.class),
							examples = @ExampleObject(value = """
									{
									  "isSuccess": false,
									  "code": "PRIVACY_CONSENT_VERSION_MISMATCH",
									  "message": "현재 개인정보 처리 동의 버전과 일치하지 않습니다.",
									  "result": null
									}
									""")
					)
			),
			@ApiResponse(
					responseCode = "401",
					description = "인증 실패",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "403",
					description = "활성 상태가 아닌 계정",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			),
			@ApiResponse(
					responseCode = "404",
					description = "사용자를 찾을 수 없음",
					content = @Content(schema = @Schema(implementation = BaseResponse.class))
			)
	})
	@PutMapping(
			value = "/me/consents",
			consumes = "application/json",
			produces = "application/json"
	)
	public BaseResponse<UserConsentResponse> updateConsents(
			@Valid @RequestBody UserConsentUpdateRequest request
	) {
		return BaseResponse.success(userConsentService.updateConsents(request));
	}
}
