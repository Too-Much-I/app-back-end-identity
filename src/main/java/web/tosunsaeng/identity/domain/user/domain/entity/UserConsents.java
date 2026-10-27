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
	private boolean marketingPushConsented;
	private String marketingPushConsentVersion;
	private Instant marketingPushConsentedAt;
	private Instant marketingPushRevokedAt;

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
		).copyMarketingFrom(this);
	}

	public UserConsents renewRequiredConsents(
			String requiredPrivacyVersion,
			String requiredTermVersion,
			Instant consentedAt
	) {
		String privacyVersion = requireVersion(
				requiredPrivacyVersion,
				"privacyConsentVersion"
		);
		String termVersion = requireVersion(requiredTermVersion, "termConsentVersion");
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
		if (privacyCurrent && termCurrent) {
			return this;
		}
		return new UserConsents(
				true,
				privacyVersion,
				privacyCurrent ? privacyConsentedAt : requiredConsentedAt,
				true,
				termVersion,
				termCurrent ? termConsentedAt : requiredConsentedAt,
				qualityReviewConsented,
				qualityReviewConsentVersion,
				qualityReviewConsentedAt
		).copyMarketingFrom(this);
	}

	private UserConsents copyMarketingFrom(UserConsents source) {
		marketingPushConsented = source.marketingPushConsented;
		marketingPushConsentVersion = source.marketingPushConsentVersion;
		marketingPushConsentedAt = source.marketingPushConsentedAt;
		marketingPushRevokedAt = source.marketingPushRevokedAt;
		return this;
	}

	/** null preserves existing consent, including on requests from older clients. */
	public UserConsents renewMarketingPush(Boolean requested, String version, Instant now) {
		if (requested == null) return this;
		Objects.requireNonNull(now, "consent change time");
		String requestedVersion = requested ? requireVersion(version, "marketingPushConsentVersion") : null;
		if (requested ? marketingPushConsented && requestedVersion.equals(marketingPushConsentVersion)
				&& marketingPushConsentedAt != null : !marketingPushConsented) return this;
		UserConsents copy = new UserConsents(privacyConsented, privacyConsentVersion, privacyConsentedAt,
				termConsented, termConsentVersion, termConsentedAt, qualityReviewConsented,
				qualityReviewConsentVersion, qualityReviewConsentedAt).copyMarketingFrom(this);
		copy.marketingPushConsented = requested;
		if (requested) {
			copy.marketingPushConsentVersion = requestedVersion;
			copy.marketingPushConsentedAt = now;
		} else {
			// Retain last consent evidence and record the latest explicit withdrawal.
			copy.marketingPushRevokedAt = now;
		}
		return copy;
	}

	public boolean isMarketingPushConsented() { return marketingPushConsented; }
	public String getMarketingPushConsentVersion() { return marketingPushConsentVersion; }
	public Instant getMarketingPushConsentedAt() { return marketingPushConsentedAt; }
	public Instant getMarketingPushRevokedAt() { return marketingPushRevokedAt; }

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
