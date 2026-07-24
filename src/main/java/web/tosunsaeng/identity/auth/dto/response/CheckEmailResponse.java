package web.tosunsaeng.identity.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckEmailResponse(
		@JsonProperty("isAvailable") boolean isAvailable,
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
