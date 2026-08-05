package web.tosunsaeng.identity.domain.user.domain;

import java.time.Instant;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import web.tosunsaeng.identity.domain.user.domain.entity.UserConsents;
import web.tosunsaeng.identity.domain.user.exception.UserErrorStatus;
import web.tosunsaeng.identity.domain.user.exception.UserException;

@Component
public class ConsentPolicy {

	private final String privacyConsentVersion;
	private final String termConsentVersion;

	public ConsentPolicy(
			@Value("${app.consent.privacy-version}") String privacyConsentVersion,
			@Value("${app.consent.term-version}") String termConsentVersion
	) {
		this.privacyConsentVersion = requireVersion(
				privacyConsentVersion,
				"privacyConsentVersion"
		);
		this.termConsentVersion = requireVersion(termConsentVersion, "termConsentVersion");
	}

	public void validate(
			Boolean privacyConsented,
			String requestedPrivacyVersion,
			Boolean termConsented,
			String requestedTermVersion
	) {
		if (!Boolean.TRUE.equals(privacyConsented)) {
			throw new UserException(UserErrorStatus.PRIVACY_CONSENT_REQUIRED);
		}
		if (!privacyConsentVersion.equals(trimNullable(requestedPrivacyVersion))) {
			throw new UserException(UserErrorStatus.PRIVACY_CONSENT_VERSION_MISMATCH);
		}
		if (!Boolean.TRUE.equals(termConsented)) {
			throw new UserException(UserErrorStatus.TERM_CONSENT_REQUIRED);
		}
		if (!termConsentVersion.equals(trimNullable(requestedTermVersion))) {
			throw new UserException(UserErrorStatus.TERM_CONSENT_VERSION_MISMATCH);
		}
	}

	public UserConsents consentedAt(Instant consentedAt) {
		return UserConsents.consented(
				privacyConsentVersion,
				termConsentVersion,
				consentedAt
		);
	}

	public String getPrivacyConsentVersion() {
		return privacyConsentVersion;
	}

	public String getTermConsentVersion() {
		return termConsentVersion;
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

	private static String trimNullable(String value) {
		return value == null ? null : value.trim();
	}
}
