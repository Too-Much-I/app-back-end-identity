package web.tosunsaeng.identity.domain.auth.registration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Guest 사용자 생성 및 인증 토큰 발급 요청")
public record GuestAuthRequest(
		@Schema(
				description = "앱이 생성한 UUID v4 설치 식별자. 인증 자격 증명이 아닌 중복 방지 식별자입니다.",
				example = "550e8400-e29b-41d4-a716-446655440000"
		)
		@NotBlank(message = "설치 ID는 필수입니다.")
		@Size(min = 36, max = 36, message = "설치 ID는 표준 UUID 길이여야 합니다.")
		@Pattern(
				regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89aAbB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$",
				message = "설치 ID는 UUID v4 형식이어야 합니다."
		)
		String installationId,

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
	private static final int MAX_RAW_INSTALLATION_ID_LENGTH = 64;

	public GuestAuthRequest {
		if (installationId != null
				&& installationId.length() <= MAX_RAW_INSTALLATION_ID_LENGTH) {
			installationId = installationId.trim();
		}
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
		return "GuestAuthRequest[installationId=redacted, consents=redacted]";
	}
}
