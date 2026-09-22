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
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderChangeService;
import web.tosunsaeng.identity.domain.auth.providerchange.ProviderLinkService;
import web.tosunsaeng.identity.domain.auth.registration.dto.response.*;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.user.domain.enums.*;
import web.tosunsaeng.identity.domain.user.dto.response.*;
import web.tosunsaeng.identity.global.exception.ErrorCode;
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
		for (String path : new String[]{"signup", "guest/upgrade", "guest/merge"}) {
			success(api, FIREBASE + path, "post", "200", "MEMBER_AUTHENTICATED", "MEMBER 인증 토큰 발급",
					new FirebaseSignupResponse(ACCESS, REFRESH, "Bearer", 1800000, 1209600000));
		}
		success(api, FIREBASE + "auth-methods/sync", "post", "200", "SYNCED", "현재 연결된 SNS (신규 연결 기능 아님)",
				new FirebaseAuthMethodsSyncResponse(Set.of(SocialProvider.GOOGLE, SocialProvider.APPLE)));

		success(api, PROVIDERS + "unlink", "post", "202", "PROCESSING", "해제 접수, 3초 후 상태 조회",
				new ProviderChangeService.Status(ID, SocialProvider.GOOGLE, "PROCESSING", NOW, null, 3));
		success(api, PROVIDERS + "unlink/status", "post", "200", "PROCESSING", "원격 해제 진행 중",
				new ProviderChangeService.Status(ID, SocialProvider.GOOGLE, "PROCESSING", NOW, null, 3));
		success(api, PROVIDERS + "unlink/status", "post", "200", "COMPLETED", "원격 해제 완료, 토큰 발급 없음",
				new ProviderChangeService.Status(ID, SocialProvider.GOOGLE, "COMPLETED", NOW, NOW.plusSeconds(8), null));
		link(api, "prepare", "PREPARED", false, "준비 완료, 아직 SDK 연결 금지");
		link(api, "prepare", "ALREADY_LINKED", false, "현재 MEMBER에 이미 연결됨 (Guest prepare와 별개)");
		link(api, "start", "STARTED", true, "최초 start 성공에 한해 SDK 연결 허용");
		success(api, PROVIDERS + "link/start", "post", "200", "START_REPLAY", "start 재시도는 SDK 재실행 허가 아님",
				new ProviderLinkService.Status(ID, SocialProvider.GOOGLE, "STARTED", NOW.plusSeconds(300), false));
		link(api, "complete", "COMPLETED", false, "서버 연결 확정");
		for (String state : new String[]{"PREPARED", "STARTED", "COMPLETED", "ALREADY_LINKED", "EXPIRED", "ACTION_REQUIRED", "SUPERSEDED"}) {
			link(api, "status", state, false, "상태 조회는 SDK 실행을 허가하지 않음");
		}

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
		success(api, USER + "me/consents", "put", "200", "SAVED", "현재 동의 저장 완료",
				new UserConsentResponse(true, "privacy-v1", NOW, true, "term-v1", NOW, false, null, null));
		success(api, USER + "withdraw", "post", "200", "WITHDRAWN", "탈퇴 확정, 외부 데이터 정리는 비동기",
				new WithdrawResponse(UserStatus.WITHDRAWN, NOW));
		example(api, "/.well-known/jwks.json", "get", "200", "PUBLIC_KEYS", "공개키 구조 예시: n은 유효한 키가 아닌 자리표시자",
				Map.of("keys", java.util.List.of(Map.of("kty", "RSA", "use", "sig", "kid", "example-key-id",
						"alg", "RS256", "n", "<base64url-public-modulus>", "e", "AQAB"))));

		error(api, AUTH + "login", AuthErrorStatus.INVALID_CREDENTIALS);
		error(api, AUTH + "signup", AuthErrorStatus.EMAIL_ALREADY_EXISTS);
		error(api, AUTH + "guest", AuthErrorStatus.GUEST_ALREADY_EXISTS);
		error(api, AUTH + "reissue", AuthErrorStatus.INVALID_REFRESH_TOKEN);
		error(api, AUTH + "reissue", AuthErrorStatus.REISSUE_RECOVERY_EXPIRED);
		error(api, AUTH + "reissue", AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
		error(api, FIREBASE + "guest/prepare", AuthErrorStatus.IDENTITY_STATE_CONFLICT);
		error(api, FIREBASE + "guest/upgrade", AuthErrorStatus.FIREBASE_ENROLLMENT_CONFLICT);
		for (String path : new String[]{"unlink", "unlink/status", "link/prepare", "link/start", "link/complete", "link/status"}) {
			error(api, PROVIDERS + path, AuthErrorStatus.PROVIDER_CHANGE_UNAVAILABLE);
			error(api, PROVIDERS + path, AuthErrorStatus.PROVIDER_RATE_LIMITED);
		}
		error(api, PROVIDERS + "unlink", AuthErrorStatus.PROVIDER_LAST_METHOD);
		error(api, PROVIDERS + "link/start", AuthErrorStatus.PROVIDER_RELINK_EXPIRED);
		error(api, PROVIDERS + "link/status", AuthErrorStatus.PROVIDER_OPERATION_NOT_FOUND);
	}

	private Set<FirebaseEnrollmentRequirement> signupRequirements() {
		return Set.of(FirebaseEnrollmentRequirement.PHONE_VERIFICATION, FirebaseEnrollmentRequirement.PROFILE,
				FirebaseEnrollmentRequirement.CONSENTS);
	}

	private void link(OpenAPI api, String action, String state, boolean allowed, String summary) {
		success(api, PROVIDERS + "link/" + action, "post", "200", state, summary,
				new ProviderLinkService.Status(ID, SocialProvider.GOOGLE, state, NOW.plusSeconds(300), allowed));
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
