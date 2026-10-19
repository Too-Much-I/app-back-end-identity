package web.tosunsaeng.identity.domain.auth.phoneidentity.infrastructure;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintKeyStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKey;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprintKeyRegistry;

@ConfigurationProperties(prefix = "app.phone-eligibility-binding")
public record PhoneEligibilityBindingProperties(
		boolean enabled,
		String consumerScopeId,
		String keyRing
) {

	public PhoneEligibilityBindingProperties {
		consumerScopeId = consumerScopeId == null ? "" : consumerScopeId.trim();
		keyRing = keyRing == null ? "" : keyRing.trim();
	}

	public PhoneFingerprintKeyRegistry keyRegistry() {
		if (!enabled || consumerScopeId.isBlank() || keyRing.isBlank()) {
			throw invalidConfiguration();
		}
		try {
			List<PhoneFingerprintKey> keys = Arrays.stream(keyRing.split(";", -1))
					.map(String::trim)
					.map(PhoneEligibilityBindingProperties::parseKey)
					.toList();
			return new PhoneFingerprintKeyRegistry(keys);
		} catch (IllegalArgumentException exception) {
			throw invalidConfiguration();
		}
	}

	private static PhoneFingerprintKey parseKey(String entry) {
		String[] fields = entry.split(",", -1);
		if (fields.length != 3) {
			throw invalidConfiguration();
		}
		return PhoneFingerprintKey.fromBase64(
				fields[0].trim(),
				PhoneFingerprintKeyStatus.valueOf(fields[1].trim()),
				fields[2].trim()
		);
	}

	private static IllegalArgumentException invalidConfiguration() {
		return new IllegalArgumentException(
				"Phone eligibility binding configuration is invalid."
		);
	}

	@Override
	public String toString() {
		return "PhoneEligibilityBindingProperties[enabled=" + enabled
				+ ", consumerScopeId=" + consumerScopeId + ", keyRing=[REDACTED]]";
	}
}
