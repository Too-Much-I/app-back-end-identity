package web.tosunsaeng.identity.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import web.tosunsaeng.identity.domain.user.domain.ConsentPolicy;

@Schema(description = "가입 전 공개 조회용 현재 정책 버전. 사용자 동의 상태는 포함하지 않습니다.")
public record CurrentConsentPolicyResponse(
		@Schema(description = "현재 개인정보 처리방침 버전", example = "privacy-v1")
		String privacyConsentVersion,
		@Schema(description = "현재 이용약관 버전", example = "term-v1")
		String termConsentVersion,
		@Schema(description = "현재 학습 품질 검토 선택 동의 버전", example = "quality-review-v1")
		String qualityReviewConsentVersion
) {
	public static CurrentConsentPolicyResponse from(ConsentPolicy policy) {
		return new CurrentConsentPolicyResponse(
				policy.getPrivacyConsentVersion(),
				policy.getTermConsentVersion(),
				policy.getQualityReviewConsentVersion()
		);
	}
}
