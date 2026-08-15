package web.tosunsaeng.identity.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 필수 개인정보 처리방침과 이용약관 동의 요청")
public record UserConsentUpdateRequest(
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

		@Schema(
				description = "품질 검토 이용 선택 동의 여부. 누락하면 false로 처리합니다.",
				example = "false"
		)
		Boolean isQualityReviewConsented,

		@Schema(
				description = "품질 검토 이용 동의 정책 버전. 미동의이면 생략할 수 있습니다.",
				example = "quality-review-v1",
				types = {"string", "null"}
		)
		@Size(max = 100, message = "품질 검토 이용 동의 버전은 100자 이하여야 합니다.")
		@Pattern(
				regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$",
				message = "품질 검토 이용 동의 버전 형식이 올바르지 않습니다."
		)
		String qualityReviewConsentVersion
) {

	public UserConsentUpdateRequest {
		privacyConsentVersion = trimNullable(privacyConsentVersion);
		termConsentVersion = trimNullable(termConsentVersion);
		isQualityReviewConsented = Boolean.TRUE.equals(isQualityReviewConsented);
		qualityReviewConsentVersion = trimNullable(qualityReviewConsentVersion);
	}

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}

	@Override
	public String toString() {
		return "UserConsentUpdateRequest[redacted]";
	}
}
