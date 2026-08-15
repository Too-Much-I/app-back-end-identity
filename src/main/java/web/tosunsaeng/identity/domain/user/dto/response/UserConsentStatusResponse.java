package web.tosunsaeng.identity.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.entity.User;
import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

@Schema(description = "현재 필수·선택 정책 버전과 인증된 사용자의 동의 상태")
public record UserConsentStatusResponse(
		@Schema(description = "개인정보 처리방침 동의 상태")
		ConsentPolicyStatusResponse privacy,
		@Schema(description = "이용약관 동의 상태")
		ConsentPolicyStatusResponse terms,
		@Schema(description = "품질 검토 이용 선택 동의 상태")
		ConsentPolicyStatusResponse qualityReview
) {

	public static UserConsentStatusResponse from(
			User user,
			String currentPrivacyVersion,
			String currentTermVersion,
			String currentQualityReviewVersion
	) {
		UserConsents consents = user.getConsents();
		return new UserConsentStatusResponse(
				ConsentPolicyStatusResponse.of(
						currentPrivacyVersion,
						consents.isPrivacyConsented(),
						consents.getPrivacyConsentVersion(),
						consents.getPrivacyConsentedAt()
				),
				ConsentPolicyStatusResponse.of(
						currentTermVersion,
						consents.isTermConsented(),
						consents.getTermConsentVersion(),
						consents.getTermConsentedAt()
				),
				ConsentPolicyStatusResponse.optionalOf(
						currentQualityReviewVersion,
						consents.isQualityReviewConsented(),
						consents.getQualityReviewConsentVersion(),
						consents.getQualityReviewConsentedAt()
				)
		);
	}
}
