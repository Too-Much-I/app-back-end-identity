package web.tosunsaeng.identity.domain.auth.federation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "기존 Guest를 Firebase MEMBER로 승격하는 요청")
public record FirebaseGuestUpgradeRequest(
		@Schema(description = "Guest 승격 준비에서 발급된 enrollment UUID")
		@NotBlank(message = "가입 요청 ID는 필수입니다.")
		@Pattern(
				regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}",
				message = "가입 요청 ID 형식이 올바르지 않습니다."
		)
		String enrollmentId,

		@Schema(
				description = "phone link 이후 강제 갱신한 Firebase ID Token",
				format = "password",
				accessMode = Schema.AccessMode.WRITE_ONLY
		)
		@NotBlank(message = "Firebase 인증 정보는 필수입니다.")
		@Size(max = 16_384, message = "Firebase 인증 정보가 너무 깁니다.")
		String firebaseIdToken,

		@Schema(description = "MEMBER 닉네임", example = "토스마스터")
		@NotBlank(message = "닉네임은 필수입니다.")
		@Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다.")
		String nickname,

		@Schema(description = "개인정보 처리 동의 여부", example = "true")
		@NotNull(message = "개인정보 처리 동의 여부는 필수입니다.")
		Boolean isPrivacyConsented,

		@Schema(description = "개인정보 처리 동의 정책 버전", example = "privacy-v1")
		@NotBlank(message = "개인정보 처리 동의 버전은 필수입니다.")
		String privacyConsentVersion,

		@Schema(description = "이용약관 동의 여부", example = "true")
		@NotNull(message = "이용약관 동의 여부는 필수입니다.")
		Boolean isTermConsented,

		@Schema(description = "이용약관 정책 버전", example = "term-v1")
		@NotBlank(message = "이용약관 동의 버전은 필수입니다.")
		String termConsentVersion,

		@Schema(description = "학습 품질 검토 이용 선택 동의. 누락/null이면 기존 동의 유지, false이면 철회",
				example = "false", types = {"boolean", "null"})
		Boolean isQualityReviewConsented,

		@Schema(description = "품질 검토 정책 버전. 명시 true이면 현재 버전 필수, 그 외 생략 가능",
				example = "quality-review-v1", types = {"string", "null"})
		@Size(max = 100, message = "품질 검토 이용 동의 버전은 100자 이하여야 합니다.")
		@Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$",
				message = "품질 검토 이용 동의 버전 형식이 올바르지 않습니다.")
		String qualityReviewConsentVersion
) {

	public FirebaseGuestUpgradeRequest {
		enrollmentId = trimNullable(enrollmentId);
		nickname = trimNullable(nickname);
		privacyConsentVersion = trimNullable(privacyConsentVersion);
		termConsentVersion = trimNullable(termConsentVersion);
		// null은 기존 Guest의 선택 동의 보존을 뜻하므로 false로 기본화하지 않는다.
		qualityReviewConsentVersion = trimNullable(qualityReviewConsentVersion);
	}

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}

	@Override
	public String toString() {
		return "FirebaseGuestUpgradeRequest[enrollmentId=[REDACTED], "
				+ "firebaseIdToken=[REDACTED], nickname=[REDACTED], consents=[REDACTED]]";
	}
}
