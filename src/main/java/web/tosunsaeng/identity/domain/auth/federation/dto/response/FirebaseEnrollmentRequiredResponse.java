package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Set;
import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "신규 MEMBER 가입 절차가 필요한 Firebase 교환 결과")
public record FirebaseEnrollmentRequiredResponse(
		@Schema(description = "교환 결과 종류", allowableValues = "ENROLLMENT_REQUIRED")
		FirebaseExchangeResultType type,
		@Schema(description = "가입 시도 식별자", format = "uuid")
		String enrollmentId,
		@Schema(description = "가입 완료 전에 충족해야 할 요구사항")
		Set<FirebaseEnrollmentRequirement> missingRequirements,
		@Schema(description = "가입 시도 남은 유효 기간(밀리초)", example = "600000")
		long expiresIn
) implements FirebaseExchangeResponse {

	public FirebaseEnrollmentRequiredResponse {
		if (type != FirebaseExchangeResultType.ENROLLMENT_REQUIRED) {
			throw new IllegalArgumentException("type must be ENROLLMENT_REQUIRED");
		}
		enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
		missingRequirements = Set.copyOf(Objects.requireNonNull(
				missingRequirements,
				"missingRequirements must not be null"
		));
		if (expiresIn < 0) {
			throw new IllegalArgumentException("expiresIn must not be negative");
		}
	}

	public FirebaseEnrollmentRequiredResponse(
			String enrollmentId,
			Set<FirebaseEnrollmentRequirement> missingRequirements,
			long expiresIn
	) {
		this(
				FirebaseExchangeResultType.ENROLLMENT_REQUIRED,
				enrollmentId,
				missingRequirements,
				expiresIn
		);
	}
}
