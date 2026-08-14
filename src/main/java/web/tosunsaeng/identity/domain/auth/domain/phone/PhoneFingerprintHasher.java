package web.tosunsaeng.identity.domain.auth.domain.phone;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PhoneFingerprintHasher {

	static final String DOMAIN_SEPARATOR = "tosunsaeng:identity:phone-identity:v1";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final byte SEPARATOR = 0;

	private final PhoneFingerprintKeyRegistry keyRegistry;

	public PhoneFingerprintHasher(PhoneFingerprintKeyRegistry keyRegistry) {
		this.keyRegistry = Objects.requireNonNull(keyRegistry, "keyRegistry must not be null");
	}

	public PhoneFingerprintSet fingerprint(String normalizedE164) {
		byte[] input = domainSeparatedInput(requireNormalizedE164(normalizedE164));
		List<PhoneFingerprint> retained = new ArrayList<>();
		PhoneFingerprint active = null;
		for (PhoneFingerprintKey key : keyRegistry.retainedKeys()) {
			PhoneFingerprint fingerprint = fingerprint(key, input);
			retained.add(fingerprint);
			if (key.version().equals(keyRegistry.activeWriteKey().version())) {
				active = fingerprint;
			}
		}
		return new PhoneFingerprintSet(Objects.requireNonNull(active), retained);
	}

	private static PhoneFingerprint fingerprint(PhoneFingerprintKey key, byte[] input) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key.copyKeyMaterial(), HMAC_ALGORITHM));
			String encoded = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(input));
			return new PhoneFingerprint(key.version(), encoded);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Phone fingerprint generation is unavailable.");
		}
	}

	private static byte[] domainSeparatedInput(String normalizedE164) {
		byte[] domain = DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8);
		byte[] phone = normalizedE164.getBytes(StandardCharsets.US_ASCII);
		byte[] result = new byte[domain.length + 1 + phone.length];
		System.arraycopy(domain, 0, result, 0, domain.length);
		result[domain.length] = SEPARATOR;
		System.arraycopy(phone, 0, result, domain.length + 1, phone.length);
		return result;
	}

	private static String requireNormalizedE164(String value) {
		String required = Objects.requireNonNull(value, "normalizedE164 must not be null");
		if (!required.matches("\\+[1-9][0-9]{7,14}")) {
			throw new IllegalArgumentException("Phone fingerprint input must be normalized E.164.");
		}
		return required;
	}

	@Override
	public String toString() {
		return "PhoneFingerprintHasher[keyRegistry=" + keyRegistry + "]";
	}
}
