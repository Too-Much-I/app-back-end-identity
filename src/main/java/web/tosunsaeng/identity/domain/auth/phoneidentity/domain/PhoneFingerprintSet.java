package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import java.util.List;
import java.util.Objects;

public final class PhoneFingerprintSet {

	private final PhoneFingerprint activeWrite;
	private final List<PhoneFingerprint> retained;

	public PhoneFingerprintSet(
			PhoneFingerprint activeWrite,
			List<PhoneFingerprint> retained
	) {
		this.activeWrite = Objects.requireNonNull(
				activeWrite,
				"activeWrite must not be null"
		);
		this.retained = List.copyOf(Objects.requireNonNull(retained, "retained must not be null"));
		if (this.retained.isEmpty() || !this.retained.contains(activeWrite)) {
			throw new IllegalArgumentException("Retained fingerprints must include active write.");
		}
		long distinctVersions = this.retained.stream()
				.map(PhoneFingerprint::keyVersion)
				.distinct()
				.count();
		if (distinctVersions != this.retained.size()) {
			throw new IllegalArgumentException("Fingerprint versions must be unique.");
		}
	}

	public PhoneFingerprint activeWrite() {
		return activeWrite;
	}

	public List<PhoneFingerprint> retained() {
		return retained;
	}

	@Override
	public String toString() {
		return "PhoneFingerprintSet[activeWriteVersion=" + activeWrite.keyVersion()
				+ ", retainedCount=" + retained.size() + ", values=[REDACTED]]";
	}
}
