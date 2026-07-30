package web.tosunsaeng.identity.domain.auth.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;

@Schema(description = "회원가입된 사용자 계정 정보")
public record SignupResponse(
		@Schema(description = "Identity 사용자 UUID", format = "uuid")
		String userId,
		@Schema(description = "표시용 이메일", example = "user@example.com")
		String email,
		@Schema(description = "사용자 닉네임", example = "토스마스터")
		String nickname,
		@Schema(description = "음성 데이터 수집·이용 동의 상태", example = "true")
		@JsonProperty("isAudioConsent") boolean isAudioConsent,
		@Schema(description = "계정 생성 시각", format = "date-time")
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
