package web.tosunsaeng.identity.user.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import web.tosunsaeng.identity.user.domain.User;
import web.tosunsaeng.identity.user.domain.UserProvider;

public record UserProfileResponse(
		String userId,
		String email,
		String nickname,
		UserProvider provider,
		@JsonProperty("isAudioConsent") boolean isAudioConsent,
		Instant createdAt
) {

	public static UserProfileResponse from(User user) {
		return new UserProfileResponse(
				user.getUserId(),
				user.getEmail(),
				user.getNickname(),
				user.getProvider(),
				user.getAudioConsent().isAgreed(),
				user.getCreatedAt()
		);
	}
}
