package web.tosunsaeng.identity.domain.auth.domain.phone;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;

public final class PhoneFingerprintKeyRegistry {

	private final PhoneFingerprintKey activeWriteKey;
	private final List<PhoneFingerprintKey> retainedKeys;

	public PhoneFingerprintKeyRegistry(List<PhoneFingerprintKey> keys) {
		List<PhoneFingerprintKey> required = List.copyOf(
				Objects.requireNonNull(keys, "keys must not be null")
		);
		Map<String, PhoneFingerprintKey> keysByVersion = new LinkedHashMap<>();
		for (PhoneFingerprintKey key : required) {
			PhoneFingerprintKey existing = keysByVersion.putIfAbsent(
					Objects.requireNonNull(key, "key must not be null").version(),
					key
			);
			if (existing != null) {
				throw new IllegalArgumentException(
						"Phone fingerprint key versions must be unique."
				);
			}
		}
		List<PhoneFingerprintKey> activeKeys = required.stream()
				.filter(key -> key.status() == PhoneFingerprintKeyStatus.ACTIVE_WRITE)
				.toList();
		if (activeKeys.size() != 1) {
			throw new IllegalArgumentException(
					"Exactly one active phone fingerprint write key is required."
			);
		}
		this.activeWriteKey = activeKeys.getFirst();
		this.retainedKeys = List.copyOf(keysByVersion.values());
	}

	public PhoneFingerprintKey activeWriteKey() {
		return activeWriteKey;
	}

	public List<PhoneFingerprintKey> retainedKeys() {
		return retainedKeys;
	}

	@Override
	public String toString() {
		return "PhoneFingerprintKeyRegistry[activeWriteVersion="
				+ activeWriteKey.version() + ", retainedKeyCount="
				+ retainedKeys.size() + ", keyMaterial=[REDACTED]]";
	}
}
