package web.tosunsaeng.identity.domain.auth.federation.api;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseExchangeUseCase;
import web.tosunsaeng.identity.domain.auth.federation.application.FirebaseSignupUseCase;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseExchangeRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.request.FirebaseSignupRequest;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseExchangeResponse;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseExchangeResponseEnvelope;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.FirebaseSignupResponse;
import web.tosunsaeng.identity.global.response.BaseResponse;

@Tag(name = "Firebase Auth", description = "Firebase credential과 Identity 인증·가입 연결 API")
@RestController
@RequestMapping("/api/v1/auth/firebase")
@RequiredArgsConstructor
public class FirebaseExchangeController {

	private final FirebaseExchangeUseCase firebaseExchangeUseCase;
	private final FirebaseSignupUseCase firebaseSignupUseCase;

	@Operation(
			summary = "Firebase 로그인 교환",
			description = "Firebase ID Token을 검증해 기존 MEMBER에는 Identity Token을 발급하고, "
					+ "미등록 UID에는 User를 생성하지 않은 채 가입 시도를 반환합니다."
	)
	@ApiResponses({
			@ApiResponse(
					responseCode = "200",
					description = "AUTHENTICATED 또는 ENROLLMENT_REQUIRED",
					content = @Content(
							schema = @Schema(implementation = FirebaseExchangeResponseEnvelope.class),
							examples = {
							@ExampleObject(name = "AUTHENTICATED", value = """
									{
									  "isSuccess": true,
									  "code": "SUCCESS",
									  "message": "요청에 성공했습니다.",
									  "result": {
									    "type": "AUTHENTICATED",
									    "accessToken": "<redacted>",
									    "refreshToken": "<redacted>",
									    "grantType": "Bearer",
									    "accessTokenExpiresIn": 1800000,
									    "refreshTokenExpiresIn": 1209600000
									  }
									}
									"""),
							@ExampleObject(name = "ENROLLMENT_REQUIRED", value = """
									{
									  "isSuccess": true,
									  "code": "SUCCESS",
									  "message": "요청에 성공했습니다.",
									  "result": {
									    "type": "ENROLLMENT_REQUIRED",
									    "enrollmentId": "550e8400-e29b-41d4-a716-446655440000",
									    "missingRequirements": ["PHONE_VERIFICATION", "PROFILE", "CONSENTS"],
									    "expiresIn": 600000
									  }
									}
									""")
							}
					)
			),
			@ApiResponse(responseCode = "400", description = "요청 JSON 형식 오류", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "401", description = "Firebase 인증 실패 또는 recent-auth 필요", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "403", description = "비활성 계정 또는 허용되지 않은 Provider·phone 로그인", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "409", description = "Firebase 또는 Social identity 소유권 충돌", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "429", description = "Firebase 요청 제한", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "503", description = "Firebase 기능 비활성 또는 일시 장애", content = @Content(schema = @Schema(implementation = BaseResponse.class)))
	})
	@PostMapping(value = "/exchange", consumes = "application/json", produces = "application/json")
	public BaseResponse<FirebaseExchangeResponse> exchange(
			@Valid @RequestBody FirebaseExchangeRequest request
	) {
		return BaseResponse.success(firebaseExchangeUseCase.exchange(request));
	}

	@Operation(
			summary = "Firebase 신규 MEMBER 가입 완료",
			description = "fresh Firebase credential과 유효한 enrollment를 검증하고 phone·동의가 "
					+ "충족된 신규 MEMBER aggregate를 단일 Transaction으로 생성합니다."
	)
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "가입 및 Identity Token 발급 성공"),
			@ApiResponse(responseCode = "400", description = "요청·profile·phone 형식 오류", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "401", description = "Firebase 인증 실패 또는 recent-auth 필요", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "403", description = "provider·phone·email proof 또는 동의 요건 미충족", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "409", description = "enrollment·Firebase·Social·phone 소유권 충돌", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "429", description = "Firebase 요청 제한", content = @Content(schema = @Schema(implementation = BaseResponse.class))),
			@ApiResponse(responseCode = "503", description = "Firebase 기능 비활성 또는 일시 장애", content = @Content(schema = @Schema(implementation = BaseResponse.class)))
	})
	@PostMapping(value = "/signup", consumes = "application/json", produces = "application/json")
	public BaseResponse<FirebaseSignupResponse> signup(
			@Valid @RequestBody FirebaseSignupRequest request
	) {
		return BaseResponse.success(firebaseSignupUseCase.signup(request));
	}
}
