package web.tosunsaeng.identity.global.config;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;
import web.tosunsaeng.identity.domain.auth.federation.dto.response.*;
import web.tosunsaeng.identity.domain.auth.local.dto.response.LoginResponse;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.*;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.user.domain.enums.*;
import web.tosunsaeng.identity.domain.user.dto.response.*;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;
import web.tosunsaeng.identity.global.exception.ErrorCode;
import web.tosunsaeng.identity.global.exception.ValidationErrorDetail;
import web.tosunsaeng.identity.global.response.BaseResponse;

/** Live Swagger와 정적 공유본에 동일한 DTO 기반 가상 응답 예시를 제공한다. */
@Component
public class IdentityOpenApiExamples implements OpenApiCustomizer {

	private static final String AUTH = "/api/v1/auth/";
	private static final String FIREBASE = AUTH + "firebase/";
	private static final String PROVIDERS = FIREBASE + "providers/";
	private static final String USER = "/api/v1/users/";
	private static final String ID = "11111111-1111-4111-8111-111111111111";
	private static final Instant NOW = Instant.parse("2026-09-21T00:00:00Z");
	private static final String ACCESS = "<example-access-token-not-valid>";
	private static final String REFRESH = "<example-refresh-token-not-valid>";
	private final ObjectMapper mapper;

	public IdentityOpenApiExamples(ObjectMapper mapper) { this.mapper = mapper; }

	@Override
	public void customise(OpenAPI api) {
		String supportPath = "/api/v1/support/inquiries";
		for (String status : new String[]{"200", "201"}) {
			success(api, supportPath, "post", status, "RECEIVED", "문의 접수 (Slack 전송과 별개)",
					new web.tosunsaeng.identity.domain.support.SupportController.Receipt(ID, "RECEIVED"));
		}
		for (var code : web.tosunsaeng.identity.domain.support.SupportError.values()) error(api, supportPath, code);
		String appVersionPath = "/api/v1/app/version";
		for (String platform : new String[]{"android", "ios"}) {
			success(api, appVersionPath, "get", "200", platform, "최신 출시 버전 예시 (실제 출시값 아님)",
					new web.tosunsaeng.identity.domain.appversion.AppVersionResponse(platform, "1.2.0"));
		}
		for (ErrorCode code : new ErrorCode[]{
				web.tosunsaeng.identity.global.exception.CommonErrorStatus.INVALID_REQUEST,
				web.tosunsaeng.identity.domain.appversion.AppVersionErrorStatus.APP_VERSION_UNAVAILABLE}) {
			String status = Integer.toString(code.getHttpStatus().value());
			media(api, appVersionPath, "get", status).setSchema(new Schema<>().$ref("#/components/schemas/BaseResponse"));
			example(api, appVersionPath, "get", status, code.getCode(), code.getMessage(), BaseResponse.failure(code));
		}
		success(api, AUTH + "check-email", "post", "200", "AVAILABLE", "사용 가능한 이메일", CheckEmailResponse.from(true));
		success(api, AUTH + "check-email", "post", "200", "UNAVAILABLE", "이미 사용 중인 이메일", CheckEmailResponse.from(false));
		success(api, AUTH + "signup", "post", "200", "SIGNED_UP", "이메일 가입 완료 (토큰 발급 아님)",
				new SignupResponse(ID, "user@example.com", "예시회원", true, "privacy-v1", NOW, true, "term-v1", NOW, NOW));
		success(api, AUTH + "guest", "post", "200", "GUEST_CREATED", "Guest 계정과 세션 생성",
				new GuestAuthResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000));
		success(api, AUTH + "login", "post", "200", "LOGGED_IN", "이메일 로그인 완료",
				new LoginResponse(ACCESS, REFRESH, "Bearer", 1800000));
		success(api, AUTH + "reissue", "post", "200", "ROTATED", "새 토큰 발급 (유효 기간은 밀리초)",
				new ReissueResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000));
		for (String path : new String[]{"logout", "logout-all"}) {
			success(api, AUTH + path, "post", "200", "ACCEPTED", "내부 세션 폐기 완료 (원격 완료와 별개)", null);
		}
		success(api, FIREBASE + "exchange", "post", "200", "AUTHENTICATED", "기존 MEMBER 로그인",
				new FirebaseAuthenticatedResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000));
		success(api, FIREBASE + "exchange", "post", "200", "ENROLLMENT_REQUIRED", "전화번호·프로필·동의가 필요한 가입",
				new FirebaseEnrollmentRequiredResponse(ID, signupRequirements(), 600000));
		success(api, FIREBASE + "guest/prepare", "post", "200", "ENROLLMENT_REQUIRED", "전화번호 인증과 프로필 입력·동의 필요",
				FirebaseGuestPrepareResponse.enrollmentRequired(ID, signupRequirements(), "privacy-v1", "term-v1", 600000));
		success(api, FIREBASE + "guest/prepare", "post", "200", "RESUME_PROFILE", "인증·동의 충족 후 재개: 프로필 필요, TTL 비연장",
				FirebaseGuestPrepareResponse.enrollmentRequired(ID, Set.of(FirebaseEnrollmentRequirement.PROFILE), "privacy-v1", "term-v1", 240000));
		success(api, FIREBASE + "guest/prepare", "post", "200", "MERGE_REQUIRED", "다른 MEMBER 소유 SNS: merge 흐름 사용",
				FirebaseGuestPrepareResponse.mergeRequired());
		for (String path : new String[]{"signup", "guest/upgrade"}) {
			success(api, FIREBASE + path, "post", "200", "MEMBER_AUTHENTICATED", "MEMBER 인증 토큰 발급",
					new FirebaseSignupResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000));
		}

        success(api, FIREBASE + "guest/merge", "post", "200", "MEMBER_AUTHENTICATED", "병합 확정; 이전 완료는 별도 조회",
                new FirebaseGuestMergeResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000, ID));
        var progress = new web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse(
                ID, web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.Status.COMPLETED, NOW, NOW.plusSeconds(3),
                new web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.Component(
                        web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.ComponentStatus.COMPLETED, NOW.plusSeconds(3)),
                new web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.Component(
                        web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.ComponentStatus.NOT_REQUIRED, null), null);
        success(api, USER + "me/merges/{mergeId}", "get", "200", "COMPLETED", "첫 출시 LC 완료; Billing 대상 제외", progress);
        success(api, USER + "me/merges", "get", "200", "PAGE", "activeOnly=false 완료 포함 목록",
                new web.tosunsaeng.identity.domain.auth.mergeprogress.MergeProgressResponse.Page(java.util.List.of(progress), null));
        for (String path : new String[]{USER + "me/merges", USER + "me/merges/{mergeId}"}) {
            for (ErrorCode code : new ErrorCode[]{AuthErrorStatus.INVALID_MERGE_STATUS_REQUEST,
                    AuthErrorStatus.MERGE_STATUS_NOT_FOUND, AuthErrorStatus.MERGE_STATUS_RATE_LIMITED,
                    AuthErrorStatus.MERGE_STATUS_UNAVAILABLE, web.tosunsaeng.identity.global.exception.CommonErrorStatus.UNAUTHORIZED,
                    web.tosunsaeng.identity.domain.user.exception.UserErrorStatus.ACCOUNT_NOT_ACTIVE}) {
                String status = Integer.toString(code.getHttpStatus().value());
                media(api, path, "get", status).setSchema(new Schema<>().$ref("#/components/schemas/BaseResponse"));
                example(api, path, "get", status, code.getCode(), code.getMessage(), BaseResponse.failure(code));
            }
        }
		success(api, FIREBASE + "auth-methods/sync", "post", "200", "SYNCED", "현재 연결된 SNS (신규 연결 기능 아님)",
				new FirebaseAuthMethodsSyncResponse(Set.of(SocialProvider.GOOGLE)));

		success(api, AUTH + "account-recovery/prepare", "post", "200", "PREPARED", "SMS 인증 전 접수",
				new web.tosunsaeng.identity.domain.auth.accountrecovery.AccountRecoveryService.Prepared(ID, NOW.plusSeconds(300)));
		success(api, AUTH + "account-recovery/lookup", "post", "200", "FOUND", "마스킹 계정 힌트; 로그인 토큰 없음",
				new web.tosunsaeng.identity.domain.auth.accountrecovery.RecoveryResult(
						web.tosunsaeng.identity.domain.auth.accountrecovery.RecoveryResult.Status.FOUND, SocialProvider.GOOGLE,
						"u***@example.com", web.tosunsaeng.identity.domain.auth.domain.EmailHint.Kind.EMAIL));
		success(api, AUTH + "account-recovery/lookup", "post", "200", "NOT_FOUND", "현재 가입 계정 없음",
				web.tosunsaeng.identity.domain.auth.accountrecovery.RecoveryResult.notFound());
		success(api, AUTH + "account-recovery/lookup", "post", "200", "ACTION_REQUIRED", "고객 지원 확인 필요",
				web.tosunsaeng.identity.domain.auth.accountrecovery.RecoveryResult.actionRequired());

		success(api, USER + "me", "get", "200", "MEMBER", "SNS MEMBER 프로필",
				new UserProfileResponse(ID, "user@example.com", "예시회원", UserAccountType.MEMBER, UserProvider.FEDERATED,
						true, "privacy-v1", NOW, true, "term-v1", NOW, NOW));
		success(api, USER + "me", "get", "200", "GUEST", "Guest 프로필 (가입 재개 판단은 guest/prepare 사용)",
				new UserProfileResponse(ID, null, "예시게스트", UserAccountType.GUEST, UserProvider.GUEST,
						true, "privacy-v1", NOW, true, "term-v1", NOW, NOW));
		success(api, USER + "me/consents", "get", "200", "CURRENT", "필수 동의 충족, 선택 동의 미동의",
				new UserConsentStatusResponse(ConsentPolicyStatusResponse.of("privacy-v1", true, "privacy-v1", NOW),
						ConsentPolicyStatusResponse.of("term-v1", true, "term-v1", NOW),
						ConsentPolicyStatusResponse.optionalOf("quality-review-v1", false, null, null)));
		success(api, "/api/v1/policies/consents", "get", "200", "CURRENT_POLICY_VERSIONS", "인증 없는 현재 정책 버전 조회",
				new CurrentConsentPolicyResponse("privacy-v1", "term-v1", "quality-review-v1"));
		success(api, USER + "me/consents", "put", "200", "SAVED", "현재 동의 저장 완료",
				new UserConsentResponse(true, "privacy-v1", NOW, true, "term-v1", NOW, false, null, null));
		success(api, USER + "withdraw", "post", "200", "WITHDRAWN", "탈퇴 확정, 외부 데이터 정리는 비동기",
				new WithdrawResponse(UserStatus.WITHDRAWN, NOW));
		withdrawalExamples(api);
		example(api, "/.well-known/jwks.json", "get", "200", "PUBLIC_KEYS", "공개키 구조 예시: n은 유효한 키가 아닌 자리표시자",
				Map.of("keys", java.util.List.of(Map.of("kty", "RSA", "use", "sig", "kid", "example-key-id",
						"alg", "RS256", "n", "<base64url-public-modulus>", "e", "AQAB"))));

		error(api, AUTH + "login", AuthErrorStatus.INVALID_CREDENTIALS);
		for (String path : new String[]{"account-recovery/prepare", "account-recovery/lookup"}) {
			error(api, AUTH + path, AuthErrorStatus.RECOVERY_RATE_LIMITED);
			error(api, AUTH + path, AuthErrorStatus.RECOVERY_UNAVAILABLE);
		}
		for (var code : new AuthErrorStatus[]{AuthErrorStatus.INVALID_RECOVERY_REQUEST, AuthErrorStatus.INVALID_RECOVERY_PROOF,
				AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED, AuthErrorStatus.RECOVERY_CONFLICT, AuthErrorStatus.RECOVERY_EXPIRED}) {
			error(api, AUTH + "account-recovery/lookup", code);
		}
		for (String path : new String[]{"signup", "guest/upgrade"}) error(api, FIREBASE + path, AuthErrorStatus.SINGLE_SNS_REQUIRED);
		for (String path : new String[]{"exchange", "guest/merge", "auth-methods/sync"}) error(api, FIREBASE + path, AuthErrorStatus.SNS_ACCOUNT_MISMATCH);
		error(api, AUTH + "signup", AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		error(api, AUTH + "guest", AuthErrorStatus.GUEST_ALREADY_EXISTS);
		error(api, AUTH + "reissue", AuthErrorStatus.INVALID_REFRESH_TOKEN);
		error(api, AUTH + "reissue", AuthErrorStatus.REISSUE_RECOVERY_EXPIRED);
		error(api, AUTH + "reissue", AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
		error(api, FIREBASE + "guest/prepare", AuthErrorStatus.IDENTITY_STATE_CONFLICT);
		error(api, FIREBASE + "guest/upgrade", AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
		error(api, FIREBASE + "guest/merge", AuthErrorStatus.GUEST_MERGE_TARGET_WITHDRAWN);
		error(api, FIREBASE + "guest/merge", AuthErrorStatus.GUEST_MERGE_TARGET_NOT_ACTIVE);
		error(api, FIREBASE + "guest/merge", AuthErrorStatus.GUEST_MERGE_CONFLICT);
		error(api, FIREBASE + "guest/merge", AuthErrorStatus.WITHDRAWAL_CLEANUP_PENDING);
		for (String path : new String[]{"unlink", "unlink/status", "link/prepare", "link/start", "link/complete", "link/status", "link/cancel", "link/failure-report", "link/pending"}) {
			api.getPaths().get(PROVIDERS + path).getPost().getResponses().remove("200");
			api.getPaths().get(PROVIDERS + path).getPost().getResponses().remove("202");
			api.getPaths().get(PROVIDERS + path).getPost().setDeprecated(true);
			api.getPaths().get(PROVIDERS + path).getPost().setDescription("폐지: 단일 SNS 정책. 유효한 요청은 410 PROVIDER_LINK_RETIRED. Guest prepare/upgrade/merge는 유지됩니다.");
			error(api, PROVIDERS + path, AuthErrorStatus.PROVIDER_LINK_RETIRED);
			error(api, PROVIDERS + path, AuthErrorStatus.PROVIDER_RATE_LIMITED);
		}
	}

	private void withdrawalExamples(OpenAPI api) {
		String path = USER + "withdraw";
		MediaType request = api.getPaths().get(path).getPost().getRequestBody().getContent().get("application/json");
		// 요청 DTO의 WRITE_ONLY 필드는 응답 직렬화 시 빠지므로 전송할 필드만 명시한다.
		request.addExamples("SNS", new Example().summary("SNS 회원 — 비밀번호 생략, 최근 재로그인 불필요")
				.value(Map.of("refreshToken", REFRESH, "firebaseIdToken", "<example-firebase-id-token-not-valid>")));
		request.addExamples("LOCAL", new Example().summary("이메일 회원 — 현재 비밀번호, Firebase 인증 정보 생략")
				.value(Map.of("refreshToken", REFRESH, "password", "<example-password-not-valid>")));
		request.addExamples("GUEST", new Example().summary("Guest — Refresh Token만 전송")
				.value(Map.of("refreshToken", REFRESH)));
		for (ErrorCode code : new ErrorCode[]{CommonErrorStatus.INVALID_REQUEST,
				UserErrorStatus.WITHDRAWAL_PASSWORD_REQUIRED, AuthErrorStatus.WITHDRAWAL_FIREBASE_PROOF_REQUIRED,
				AuthErrorStatus.WITHDRAWAL_CREDENTIAL_TYPE_MISMATCH, CommonErrorStatus.UNAUTHORIZED,
				AuthErrorStatus.INVALID_WITHDRAWAL_CREDENTIALS, AuthErrorStatus.INVALID_FIREBASE_ID_TOKEN,
				AuthErrorStatus.FIREBASE_ACCOUNT_NOT_ALLOWED, AuthErrorStatus.FIREBASE_PROVIDER_NOT_ALLOWED,
				UserErrorStatus.USER_NOT_FOUND, UserErrorStatus.WITHDRAWAL_CONFLICT,
				UserErrorStatus.WITHDRAWAL_LIFECYCLE_CONFLICT, AuthErrorStatus.FIREBASE_IDENTITY_CONFLICT,
				AuthErrorStatus.FIREBASE_RATE_LIMITED, AuthErrorStatus.FIREBASE_UNAVAILABLE,
				AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE}) {
			error(api, path, code);
			media(api, path, "post", Integer.toString(code.getHttpStatus().value()))
					.setSchema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"));
		}
		example(api, path, "post", "400", "VALIDATION_ERROR", "필수 Refresh Token 누락 — 민감한 거부값은 노출하지 않음",
				BaseResponse.failure(CommonErrorStatus.INVALID_REQUEST,
						java.util.List.of(new ValidationErrorDetail("refreshToken", null, "Refresh Token은 필수입니다."))));
	}

	private Set<FirebaseEnrollmentRequirement> signupRequirements() {
		return Set.of(FirebaseEnrollmentRequirement.PHONE_VERIFICATION, FirebaseEnrollmentRequirement.PROFILE,
				FirebaseEnrollmentRequirement.CONSENTS);
	}

	private void success(OpenAPI api, String path, String method, String status, String name, String summary, Object result) {
		MediaType media = media(api, path, method, status);
		if (media.getSchema() == null && result != null) {
			// 일부 @Content(examples=...)는 springdoc의 반환 타입 추론을 막는다.
			var type = ResolvableType.forClassWithGenerics(BaseResponse.class, result.getClass()).getType();
			var resolved = ModelConverters.getInstance().resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true));
			resolved.referencedSchemas.forEach(api.getComponents()::addSchemas);
			media.setSchema(resolved.schema);
		}
		example(api, path, method, status, name, summary, BaseResponse.success(result));
	}

	private void error(OpenAPI api, String path, ErrorCode code) {
		String status = Integer.toString(code.getHttpStatus().value());
		media(api, path, "post", status).setSchema(new Schema<>().$ref("#/components/schemas/BaseResponse"));
		example(api, path, "post", status, code.getCode(), code.getMessage(), BaseResponse.failure(code));
	}

	private void example(OpenAPI api, String path, String method, String status, String name, String summary, Object value) {
		MediaType media = media(api, path, method, status);
		media.setExample(null);
		media.addExamples(name, new Example().summary(summary).value(mapper.valueToTree(value)));
	}

	private MediaType media(OpenAPI api, String path, String method, String status) {
		Operation operation = api.getPaths().get(path).readOperationsMap().entrySet().stream()
				.filter(entry -> entry.getKey().name().equalsIgnoreCase(method)).findFirst().orElseThrow().getValue();
		ApiResponse response = operation.getResponses().computeIfAbsent(status, key -> new ApiResponse().description("응답 예시 (주요 경우)"));
		if (response.getContent() == null) response.setContent(new Content());
		Content content = response.getContent();
		if (!content.containsKey("application/json")) {
			MediaType wildcard = content.remove("*/*");
			content.addMediaType("application/json", wildcard == null ? new MediaType() : wildcard);
		}
		return content.get("application/json");
	}
}
