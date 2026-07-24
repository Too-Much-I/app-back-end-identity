package web.tosunsaeng.identity.user.domain;

import java.time.Instant;
import java.util.Objects;

public class AudioConsent {

	private boolean agreed;

	private String policyVersion;

	private Instant agreedAt;

	private Instant withdrawnAt;

	private AudioConsent() {
	}

	private AudioConsent(
			boolean agreed,
			String policyVersion,
			Instant agreedAt,
			Instant withdrawnAt
	) {
		this.agreed = agreed;
		this.policyVersion = requirePolicyVersion(policyVersion);
		this.agreedAt = Objects.requireNonNull(agreedAt, "agreedAt must not be null");
		this.withdrawnAt = withdrawnAt;
	}

	static AudioConsent agreed(String policyVersion, Instant agreedAt) {
		return new AudioConsent(true, policyVersion, agreedAt, null);
	}

	private static String requirePolicyVersion(String policyVersion) {
		String requiredPolicyVersion = Objects.requireNonNull(
				policyVersion,
				"policyVersion must not be null"
		).trim();
		if (requiredPolicyVersion.isEmpty()) {
			throw new IllegalArgumentException("policyVersion must not be blank");
		}
		return requiredPolicyVersion;
	}

	public boolean isAgreed() {
		return agreed;
	}

	public String getPolicyVersion() {
		return policyVersion;
	}

	public Instant getAgreedAt() {
		return agreedAt;
	}

	public Instant getWithdrawnAt() {
		return withdrawnAt;
	}
}
