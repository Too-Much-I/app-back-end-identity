package web.tosunsaeng.identity.domain.user.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "개별 필수 또는 선택 정책의 현재 버전과 사용자 동의 상태")
public record ConsentPolicyStatusResponse(
		@Schema(description = "서버의 현재 정책 버전", example = "privacy-v2")
		String currentVersion,
		@Schema(description = "현재 정책 버전에 유효한 사용자 동의 여부", example = "true")
		boolean consented,
		@Schema(
				description = "사용자가 마지막으로 동의한 정책 버전. 미동의 사용자는 null",
				example = "privacy-v1",
				types = {"string", "null"}
		)
		String consentedVersion,
		@Schema(
				description = "사용자가 해당 버전에 동의한 서버 시각. 미동의 사용자는 null",
				format = "date-time",
				types = {"string", "null"}
		)
		Instant consentedAt,
		@Schema(
				description = "현재 정책 버전에 대한 신규 필수 동의 필요 여부. 선택 정책은 항상 false",
				example = "true"
		)
		boolean requiresConsent
) {

	public static ConsentPolicyStatusResponse of(
			String currentVersion,
			boolean consented,
			String consentedVersion,
			Instant consentedAt
	) {
		boolean requiresConsent = !consented
				|| consentedVersion == null
				|| consentedVersion.isBlank()
				|| !currentVersion.equals(consentedVersion);
		return new ConsentPolicyStatusResponse(
				currentVersion,
				consented,
				consentedVersion,
				consentedAt,
				requiresConsent
		);
	}

	public static ConsentPolicyStatusResponse optionalOf(
			String currentVersion,
			boolean consented,
			String consentedVersion,
			Instant consentedAt
	) {
		boolean currentlyConsented = consented
				&& currentVersion.equals(consentedVersion)
				&& consentedAt != null;
		return new ConsentPolicyStatusResponse(
				currentVersion,
				currentlyConsented,
				consentedVersion,
				consentedAt,
				false
		);
	}
}
