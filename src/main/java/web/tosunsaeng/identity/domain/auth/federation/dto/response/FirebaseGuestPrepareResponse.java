package web.tosunsaeng.identity.domain.auth.federation.dto.response;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Guest Firebase 승격 준비 결과")
public record FirebaseGuestPrepareResponse(
		@Schema(description = "준비 결과 종류")
		FirebaseGuestPrepareResultType type,
		@Schema(description = "승격 시도 식별자", format = "uuid", nullable = true)
		String enrollmentId,
		@Schema(description = "승격 시도 남은 유효 기간(밀리초)", nullable = true)
		Long expiresIn
) {

	public FirebaseGuestPrepareResponse {
		type = Objects.requireNonNull(type, "type must not be null");
		if (type == FirebaseGuestPrepareResultType.ENROLLMENT_REQUIRED) {
			enrollmentId = Objects.requireNonNull(enrollmentId, "enrollmentId must not be null");
			if (expiresIn == null || expiresIn < 0) {
				throw new IllegalArgumentException("expiresIn must not be negative");
			}
		} else if (enrollmentId != null || expiresIn != null) {
			throw new IllegalArgumentException("Non-enrollment result must not contain enrollment data");
		}
	}

	public static FirebaseGuestPrepareResponse enrollmentRequired(
			String enrollmentId,
			long expiresIn
	) {
		return new FirebaseGuestPrepareResponse(
				FirebaseGuestPrepareResultType.ENROLLMENT_REQUIRED,
				enrollmentId,
				expiresIn
		);
	}

	public static FirebaseGuestPrepareResponse alreadyLinked() {
		return new FirebaseGuestPrepareResponse(
				FirebaseGuestPrepareResultType.ALREADY_LINKED,
				null,
				null
		);
	}

	public static FirebaseGuestPrepareResponse mergeRequired() {
		return new FirebaseGuestPrepareResponse(
				FirebaseGuestPrepareResultType.MERGE_REQUIRED,
				null,
				null
		);
	}
}
