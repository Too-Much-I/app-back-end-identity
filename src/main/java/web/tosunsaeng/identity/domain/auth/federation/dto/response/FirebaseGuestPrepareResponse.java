package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Guest Firebase 승격 준비 결과")
public record FirebaseGuestPrepareResponse(
		@Schema(description = "준비 결과 종류")
		FirebaseGuestPrepareResultType type,
		@Schema(description = "승격 시도 식별자", format = "uuid", nullable = true)
		String enrollmentId,
		@Schema(description = "승격 완료 전에 충족해야 할 요구사항", nullable = true)
		Set<FirebaseEnrollmentRequirement> missingRequirements,
		@Schema(description = "현재 개인정보 처리 동의 정책 버전", nullable = true)
		String privacyConsentVersion,
		@Schema(description = "현재 이용약관 동의 정책 버전", nullable = true)
		String termConsentVersion,
		@Schema(description = "승격 시도 남은 유효 기간(밀리초)", nullable = true)
		Long expiresIn
) {

	public FirebaseGuestPrepareResponse {
		type = Objects.requireNonNull(type, "type must not be null");
		if (type == FirebaseGuestPrepareResultType.ENROLLMENT_REQUIRED) {
			enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
			missingRequirements = Set.copyOf(Objects.requireNonNull(
					missingRequirements,
					"missingRequirements must not be null"
			));
			privacyConsentVersion = requireVersion(
					privacyConsentVersion,
					"privacyConsentVersion"
			);
			termConsentVersion = requireVersion(termConsentVersion, "termConsentVersion");
			if (expiresIn == null || expiresIn < 0) {
				throw new IllegalArgumentException("expiresIn must not be negative");
			}
		} else if (enrollmentId != null
				|| missingRequirements != null
				|| privacyConsentVersion != null
				|| termConsentVersion != null
				|| expiresIn != null) {
			throw new IllegalArgumentException("Non-enrollment result must not contain enrollment data");
		}
	}

	public static FirebaseGuestPrepareResponse enrollmentRequired(
			String enrollmentId,
			Set<FirebaseEnrollmentRequirement> missingRequirements,
			String privacyConsentVersion,
			String termConsentVersion,
			long expiresIn
	) {
		return new FirebaseGuestPrepareResponse(
				FirebaseGuestPrepareResultType.ENROLLMENT_REQUIRED,
				enrollmentId,
				missingRequirements,
				privacyConsentVersion,
				termConsentVersion,
				expiresIn
		);
	}

	public static FirebaseGuestPrepareResponse mergeRequired() {
		return new FirebaseGuestPrepareResponse(
				FirebaseGuestPrepareResultType.MERGE_REQUIRED,
				null,
				null,
				null,
				null,
				null
		);
	}

	private static String requireVersion(String value, String fieldName) {
		String required = Objects.requireNonNull(value, fieldName + " must not be null").trim();
		if (required.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return required;
	}
}
