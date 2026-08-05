package web.tosunsaeng.identity.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
		String termConsentVersion
) {

	public UserConsentUpdateRequest {
		privacyConsentVersion = trimNullable(privacyConsentVersion);
		termConsentVersion = trimNullable(termConsentVersion);
	}

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}

	@Override
	public String toString() {
		return "UserConsentUpdateRequest[redacted]";
	}
}
