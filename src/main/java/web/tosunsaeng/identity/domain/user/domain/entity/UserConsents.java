package web.tosunsaeng.identity.domain.user.domain.entity;

import java.time.Instant;
import java.util.Objects;

public class UserConsents {

	private boolean privacyConsented;

	private String privacyConsentVersion;

	private Instant privacyConsentedAt;

	private boolean termConsented;

	private String termConsentVersion;

	private Instant termConsentedAt;

	private boolean qualityReviewConsented;

	private String qualityReviewConsentVersion;

	private Instant qualityReviewConsentedAt;

	private UserConsents() {
	}

	private UserConsents(
			boolean privacyConsented,
			String privacyConsentVersion,
			Instant privacyConsentedAt,
			boolean termConsented,
			String termConsentVersion,
			Instant termConsentedAt,
			boolean qualityReviewConsented,
			String qualityReviewConsentVersion,
			Instant qualityReviewConsentedAt
	) {
		this.privacyConsented = privacyConsented;
		this.privacyConsentVersion = privacyConsentVersion;
		this.privacyConsentedAt = privacyConsentedAt;
		this.termConsented = termConsented;
		this.termConsentVersion = termConsentVersion;
		this.termConsentedAt = termConsentedAt;
		this.qualityReviewConsented = qualityReviewConsented;
		this.qualityReviewConsentVersion = qualityReviewConsentVersion;
		this.qualityReviewConsentedAt = qualityReviewConsentedAt;
	}

	public static UserConsents consented(
			String privacyConsentVersion,
			String termConsentVersion,
			Instant consentedAt
	) {
		return consented(
				privacyConsentVersion,
				termConsentVersion,
				false,
				null,
				consentedAt
		);
	}

	public static UserConsents consented(
			String privacyConsentVersion,
			String termConsentVersion,
			boolean qualityReviewConsented,
			String qualityReviewConsentVersion,
			Instant consentedAt
	) {
		Instant requiredConsentedAt = Objects.requireNonNull(
				consentedAt,
				"consentedAt must not be null"
		);
		return new UserConsents(
				true,
				requireVersion(privacyConsentVersion, "privacyConsentVersion"),
				requiredConsentedAt,
				true,
				requireVersion(termConsentVersion, "termConsentVersion"),
				requiredConsentedAt,
				qualityReviewConsented,
				qualityReviewConsented
						? requireVersion(
								qualityReviewConsentVersion,
								"qualityReviewConsentVersion"
						)
						: null,
				qualityReviewConsented ? requiredConsentedAt : null
		);
	}

	public static UserConsents unconsented() {
		return new UserConsents(
				false,
				null,
				null,
				false,
				null,
				null,
				false,
				null,
				null
		);
	}

	public UserConsents renew(
			String requiredPrivacyVersion,
			String requiredTermVersion,
			boolean requestedQualityReviewConsent,
			String requiredQualityReviewVersion,
			Instant consentedAt
	) {
		String privacyVersion = requireVersion(
				requiredPrivacyVersion,
				"privacyConsentVersion"
		);
		String termVersion = requireVersion(requiredTermVersion, "termConsentVersion");
		String qualityReviewVersion = requireVersion(
				requiredQualityReviewVersion,
				"qualityReviewConsentVersion"
		);
		Instant requiredConsentedAt = Objects.requireNonNull(
				consentedAt,
				"consentedAt must not be null"
		);

		boolean privacyCurrent = privacyConsented
				&& privacyVersion.equals(privacyConsentVersion)
				&& privacyConsentedAt != null;
		boolean termCurrent = termConsented
				&& termVersion.equals(termConsentVersion)
				&& termConsentedAt != null;
		boolean qualityReviewCurrent = qualityReviewConsented
				&& qualityReviewVersion.equals(qualityReviewConsentVersion)
				&& qualityReviewConsentedAt != null;
		boolean qualityReviewUnchanged = requestedQualityReviewConsent
				? qualityReviewCurrent
				: !qualityReviewConsented
						&& qualityReviewConsentVersion == null
						&& qualityReviewConsentedAt == null;
		if (privacyCurrent && termCurrent && qualityReviewUnchanged) {
			return this;
		}
		Instant renewedQualityReviewAt = null;
		if (requestedQualityReviewConsent) {
			renewedQualityReviewAt = qualityReviewCurrent
					? qualityReviewConsentedAt
					: requiredConsentedAt;
		}

		return new UserConsents(
				true,
				privacyVersion,
				privacyCurrent ? privacyConsentedAt : requiredConsentedAt,
				true,
				termVersion,
				termCurrent ? termConsentedAt : requiredConsentedAt,
				requestedQualityReviewConsent,
				requestedQualityReviewConsent ? qualityReviewVersion : null,
				renewedQualityReviewAt
		);
	}

	private static String requireVersion(String version, String fieldName) {
		String requiredVersion = Objects.requireNonNull(
				version,
				fieldName + " must not be null"
		).trim();
		if (requiredVersion.isEmpty()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return requiredVersion;
	}

	public boolean isPrivacyConsented() {
		return privacyConsented;
	}

	public String getPrivacyConsentVersion() {
		return privacyConsentVersion;
	}

	public Instant getPrivacyConsentedAt() {
		return privacyConsentedAt;
	}

	public boolean isTermConsented() {
		return termConsented;
	}

	public String getTermConsentVersion() {
		return termConsentVersion;
	}

	public Instant getTermConsentedAt() {
		return termConsentedAt;
	}

	public boolean isQualityReviewConsented() {
		return qualityReviewConsented;
	}

	public String getQualityReviewConsentVersion() {
		return qualityReviewConsentVersion;
	}

	public Instant getQualityReviewConsentedAt() {
		return qualityReviewConsentedAt;
	}
}
