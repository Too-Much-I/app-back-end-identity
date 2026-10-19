package web.tosunsaeng.identity.domain.auth.registration.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

@Schema(description = "회원가입된 사용자 계정 정보")
public record SignupResponse(
		@Schema(description = "Identity 사용자 UUID", format = "uuid")
		String userId,
		@Schema(description = "표시용 이메일", example = "user@example.com")
		String email,
		@Schema(description = "사용자 닉네임", example = "토스마스터")
		String nickname,
		@Schema(description = "개인정보 처리 동의 상태", example = "true")
		boolean privacyConsented,
		@Schema(description = "개인정보 처리 동의 정책 버전", example = "privacy-v1")
		String privacyConsentVersion,
		@Schema(description = "개인정보 처리 동의 서버 시각", format = "date-time")
		Instant privacyConsentedAt,
		@Schema(description = "이용약관 동의 상태", example = "true")
		boolean termConsented,
		@Schema(description = "이용약관 정책 버전", example = "term-v1")
		String termConsentVersion,
		@Schema(description = "이용약관 동의 서버 시각", format = "date-time")
		Instant termConsentedAt,
		@Schema(description = "계정 생성 시각", format = "date-time")
		Instant createdAt
) {

	public static SignupResponse from(User user) {
		UserConsents consents = user.getConsents();
		return new SignupResponse(
				user.getUserId(),
				user.getEmail(),
				user.getNickname(),
				consents.isPrivacyConsented(),
				consents.getPrivacyConsentVersion(),
				consents.getPrivacyConsentedAt(),
				consents.isTermConsented(),
				consents.getTermConsentVersion(),
				consents.getTermConsentedAt(),
				user.getCreatedAt()
		);
	}
}
