package web.tosunsaeng.identity.auth.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import web.tosunsaeng.identity.user.domain.User;

public record SignupResponse(
		String userId,
		String email,
		String nickname,
		@JsonProperty("isAudioConsent") boolean isAudioConsent,
		Instant createdAt
) {

	public static SignupResponse from(User user) {
		return new SignupResponse(
				user.getUserId(),
				user.getEmail(),
				user.getNickname(),
				user.getAudioConsent().isAgreed(),
				user.getCreatedAt()
		);
	}
}
