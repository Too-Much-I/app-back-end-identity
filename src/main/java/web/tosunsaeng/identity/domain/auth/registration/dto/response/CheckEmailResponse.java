package web.tosunsaeng.identity.domain.auth.registration.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이메일 사용 가능 여부 확인 결과")
public record CheckEmailResponse(
		@Schema(description = "회원가입 가능 여부", example = "true")
		@JsonProperty("isAvailable") boolean isAvailable,
		@Schema(description = "사용 가능 여부 안내 메시지")
		String message
) {

	private static final String AVAILABLE_MESSAGE = "사용 가능한 이메일입니다.";
	private static final String UNAVAILABLE_MESSAGE = "이미 사용 중인 이메일입니다.";

	public static CheckEmailResponse from(boolean available) {
		return new CheckEmailResponse(
				available,
				available ? AVAILABLE_MESSAGE : UNAVAILABLE_MESSAGE
		);
	}
}
