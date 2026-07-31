package web.tosunsaeng.identity.domain.user.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.enums.UserProvider;

@Schema(description = "인증된 사용자 프로필")
public record UserProfileResponse(
		@Schema(description = "Identity 사용자 UUID", format = "uuid")
		String userId,
		@Schema(
				description = "LOCAL·소셜 사용자는 이메일 문자열이며 Guest 사용자는 null일 수 있습니다.",
				example = "user@example.com",
				format = "email",
				types = {"string", "null"}
		)
		String email,
		@Schema(description = "사용자 닉네임", example = "토스마스터")
		String nickname,
		@Schema(description = "계정 인증 제공자", example = "LOCAL", allowableValues = {"LOCAL", "GUEST"})
		UserProvider provider,
		@Schema(description = "음성 데이터 수집·이용 동의 상태", example = "true")
		@JsonProperty("isAudioConsent") boolean isAudioConsent,
		@Schema(description = "계정 생성 시각", format = "date-time")
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
