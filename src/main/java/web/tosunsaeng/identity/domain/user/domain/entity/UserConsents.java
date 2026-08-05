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

	private UserConsents() {
	}

	private UserConsents(
			boolean privacyConsented,
			String privacyConsentVersion,
			Instant privacyConsentedAt,
			boolean termConsented,
			String termConsentVersion,
			Instant termConsentedAt
	) {
		this.privacyConsented = privacyConsented;
		this.privacyConsentVersion = privacyConsentVersion;
		this.privacyConsentedAt = privacyConsentedAt;
		this.termConsented = termConsented;
		this.termConsentVersion = termConsentVersion;
		this.termConsentedAt = termConsentedAt;
	}

	public static UserConsents consented(
			String privacyConsentVersion,
			String termConsentVersion,
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
				requiredConsentedAt
		);
	}

	public static UserConsents unconsented() {
		return new UserConsents(false, null, null, false, null, null);
	}

	public UserConsents renew(
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
				termCurrent ? termConsentedAt : requiredConsentedAt
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
}
