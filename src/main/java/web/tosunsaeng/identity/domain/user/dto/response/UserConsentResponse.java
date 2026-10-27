package web.tosunsaeng.identity.domain.user.dto.response;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

@Schema(description = "저장된 개인정보 처리방침과 이용약관 동의 상태")
public record UserConsentResponse(
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
		@Schema(description = "품질 검토 이용 선택 동의 상태", example = "false")
		boolean qualityReviewConsented,
		@Schema(
				description = "품질 검토 이용 동의 정책 버전. 미동의이면 null",
				example = "quality-review-v1",
				types = {"string", "null"}
		)
		String qualityReviewConsentVersion,
		@Schema(
				description = "품질 검토 이용 동의 서버 시각. 미동의이면 null",
				format = "date-time",
				types = {"string", "null"}
		)
		Instant qualityReviewConsentedAt,
		@Schema(description = "저장된 광고성 푸시 동의 여부. 현재 버전 유효성은 동의 상태 조회 API에서 확인")
		boolean marketingPushConsented,
		@Schema(description = "마지막 광고성 푸시 동의 버전. 철회 후에도 증빙으로 유지", example = "marketing-push-v1")
		String marketingPushConsentVersion,
		@Schema(description = "마지막 광고성 푸시 동의 서버 시각")
		Instant marketingPushConsentedAt,
		@Schema(description = "마지막 광고성 푸시 철회 서버 시각")
		Instant marketingPushRevokedAt
) {
	public UserConsentResponse(boolean privacyConsented, String privacyConsentVersion, Instant privacyConsentedAt,
			boolean termConsented, String termConsentVersion, Instant termConsentedAt,
			boolean qualityReviewConsented, String qualityReviewConsentVersion, Instant qualityReviewConsentedAt) {
		this(privacyConsented, privacyConsentVersion, privacyConsentedAt, termConsented, termConsentVersion,
				termConsentedAt, qualityReviewConsented, qualityReviewConsentVersion, qualityReviewConsentedAt,
				false, null, null, null);
	}

	public static UserConsentResponse from(User user) {
		UserConsents consents = user.getConsents();
		return new UserConsentResponse(
				consents.isPrivacyConsented(),
				consents.getPrivacyConsentVersion(),
				consents.getPrivacyConsentedAt(),
				consents.isTermConsented(),
				consents.getTermConsentVersion(),
				consents.getTermConsentedAt(),
				consents.isQualityReviewConsented(),
				consents.getQualityReviewConsentVersion(),
				consents.getQualityReviewConsentedAt(),
				consents.isMarketingPushConsented(), consents.getMarketingPushConsentVersion(),
				consents.getMarketingPushConsentedAt(), consents.getMarketingPushRevokedAt()
		);
	}
}
