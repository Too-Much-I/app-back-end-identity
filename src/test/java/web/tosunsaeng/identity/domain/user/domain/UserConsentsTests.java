package web.tosunsaeng.identity.domain.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;

class UserConsentsTests {

	private static final String PRIVACY_VERSION = "privacy-v1";
	private static final String TERM_VERSION = "term-v1";
	private static final String QUALITY_REVIEW_VERSION = "quality-review-v2";
	private static final Instant PREVIOUS_AT = Instant.parse("2026-08-14T00:00:00Z");
	private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

	@Test
	void requiredConsentCreationDefaultsQualityReviewToUnconsented() {
		UserConsents consents = UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		);

		assertThat(consents.isQualityReviewConsented()).isFalse();
		assertThat(consents.getQualityReviewConsentVersion()).isNull();
		assertThat(consents.getQualityReviewConsentedAt()).isNull();
	}

	@Test
	void falseToTrueStoresCurrentVersionAndServerTime() {
		UserConsents current = UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		);

		UserConsents updated = current.renew(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				QUALITY_REVIEW_VERSION,
				NOW
		);

		assertThat(updated).isNotSameAs(current);
		assertThat(updated.isQualityReviewConsented()).isTrue();
		assertThat(updated.getQualityReviewConsentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
		assertThat(updated.getQualityReviewConsentedAt()).isEqualTo(NOW);
		assertThat(updated.getPrivacyConsentedAt()).isEqualTo(PREVIOUS_AT);
		assertThat(updated.getTermConsentedAt()).isEqualTo(PREVIOUS_AT);
	}

	@Test
	void trueCurrentToTrueIsNoOpAndPreservesTime() {
		UserConsents current = qualityReviewConsented(QUALITY_REVIEW_VERSION);

		UserConsents updated = current.renew(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				QUALITY_REVIEW_VERSION,
				NOW
		);

		assertThat(updated).isSameAs(current);
		assertThat(updated.getQualityReviewConsentedAt()).isEqualTo(PREVIOUS_AT);
	}

	@Test
	void oldVersionToCurrentVersionUsesNewServerTime() {
		UserConsents current = qualityReviewConsented("quality-review-v1");

		UserConsents updated = current.renew(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				QUALITY_REVIEW_VERSION,
				NOW
		);

		assertThat(updated.getQualityReviewConsentVersion())
				.isEqualTo(QUALITY_REVIEW_VERSION);
		assertThat(updated.getQualityReviewConsentedAt()).isEqualTo(NOW);
	}

	@Test
	void trueToFalseClearsVersionAndTime() {
		UserConsents current = qualityReviewConsented(QUALITY_REVIEW_VERSION);

		UserConsents updated = current.renew(
				PRIVACY_VERSION,
				TERM_VERSION,
				false,
				QUALITY_REVIEW_VERSION,
				NOW
		);

		assertThat(updated).isNotSameAs(current);
		assertThat(updated.isQualityReviewConsented()).isFalse();
		assertThat(updated.getQualityReviewConsentVersion()).isNull();
		assertThat(updated.getQualityReviewConsentedAt()).isNull();
	}

	@Test
	void falseToFalseIsNoOp() {
		UserConsents current = UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				PREVIOUS_AT
		);

		UserConsents updated = current.renew(
				PRIVACY_VERSION,
				TERM_VERSION,
				false,
				QUALITY_REVIEW_VERSION,
				NOW
		);

		assertThat(updated).isSameAs(current);
	}

	private UserConsents qualityReviewConsented(String version) {
		return UserConsents.consented(
				PRIVACY_VERSION,
				TERM_VERSION,
				true,
				version,
				PREVIOUS_AT
		);
	}
}
