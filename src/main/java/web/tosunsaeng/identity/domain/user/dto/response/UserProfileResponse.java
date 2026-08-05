package web.tosunsaeng.identity.domain.user.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
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
		@Schema(description = "개인정보 처리 동의 상태. 기존 미동의 사용자는 false", example = "true")
		boolean privacyConsented,
		@Schema(
				description = "개인정보 처리 동의 정책 버전. 기존 미동의 사용자는 null",
				example = "privacy-v1",
				types = {"string", "null"}
		)
		String privacyConsentVersion,
		@Schema(
				description = "개인정보 처리 동의 서버 시각. 기존 미동의 사용자는 null",
				format = "date-time",
				types = {"string", "null"}
		)
		Instant privacyConsentedAt,
		@Schema(description = "이용약관 동의 상태. 기존 미동의 사용자는 false", example = "true")
		boolean termConsented,
		@Schema(
				description = "이용약관 동의 정책 버전. 기존 미동의 사용자는 null",
				example = "term-v1",
				types = {"string", "null"}
		)
		String termConsentVersion,
		@Schema(
				description = "이용약관 동의 서버 시각. 기존 미동의 사용자는 null",
				format = "date-time",
				types = {"string", "null"}
		)
		Instant termConsentedAt,
		@Schema(description = "계정 생성 시각", format = "date-time")
		Instant createdAt
) {

	public static UserProfileResponse from(User user) {
		UserConsents consents = user.getConsents();
		return new UserProfileResponse(
				user.getUserId(),
				user.getEmail(),
				user.getNickname(),
				user.getProvider(),
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
