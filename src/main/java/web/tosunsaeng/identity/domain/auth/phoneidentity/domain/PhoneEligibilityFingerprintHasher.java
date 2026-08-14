package web.tosunsaeng.identity.domain.auth.phoneidentity.domain;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class PhoneEligibilityFingerprintHasher {

	static final String DOMAIN_SEPARATOR =
			"tosunsaeng:identity:phone-eligibility-binding:v1";
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final byte SEPARATOR = 0;

	private final String consumerScopeId;
	private final PhoneFingerprintKeyRegistry keyRegistry;

	public PhoneEligibilityFingerprintHasher(
			String consumerScopeId,
			PhoneFingerprintKeyRegistry keyRegistry
	) {
		this.consumerScopeId = requireScope(consumerScopeId);
		this.keyRegistry = Objects.requireNonNull(keyRegistry, "keyRegistry must not be null");
	}

	public List<PhoneEligibilityFingerprintCandidate> fingerprint(String normalizedE164) {
		byte[] input = domainSeparatedInput(
				consumerScopeId,
				requireNormalizedE164(normalizedE164)
		);
		List<PhoneEligibilityFingerprintCandidate> candidates = new ArrayList<>();
		for (PhoneFingerprintKey key : keyRegistry.retainedKeys()) {
			candidates.add(fingerprint(key, input));
		}
		return List.copyOf(candidates);
	}

	public String consumerScopeId() {
		return consumerScopeId;
	}

	private static PhoneEligibilityFingerprintCandidate fingerprint(
			PhoneFingerprintKey key,
			byte[] input
	) {
		try {
			Mac mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key.copyKeyMaterial(), HMAC_ALGORITHM));
			String encoded = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal(input));
			return new PhoneEligibilityFingerprintCandidate(key.version(), encoded);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Phone eligibility fingerprint generation is unavailable.");
		}
	}

	private static byte[] domainSeparatedInput(String scope, String normalizedE164) {
		byte[] domain = DOMAIN_SEPARATOR.getBytes(StandardCharsets.UTF_8);
		byte[] scopeBytes = scope.getBytes(StandardCharsets.UTF_8);
		byte[] phone = normalizedE164.getBytes(StandardCharsets.US_ASCII);
		byte[] result = new byte[domain.length + scopeBytes.length + phone.length + 2];
		int offset = 0;
		System.arraycopy(domain, 0, result, offset, domain.length);
		offset += domain.length;
		result[offset++] = SEPARATOR;
		System.arraycopy(scopeBytes, 0, result, offset, scopeBytes.length);
		offset += scopeBytes.length;
		result[offset++] = SEPARATOR;
		System.arraycopy(phone, 0, result, offset, phone.length);
		return result;
	}

	private static String requireScope(String value) {
		String required = Objects.requireNonNull(value, "consumerScopeId must not be null").trim();
		if (!required.matches("[A-Za-z0-9._:-]{1,128}")) {
			throw new IllegalArgumentException("consumerScopeId has an invalid format");
		}
		return required;
	}

	private static String requireNormalizedE164(String value) {
		String required = Objects.requireNonNull(value, "normalizedE164 must not be null");
		if (!required.matches("\\+[1-9][0-9]{7,14}")) {
			throw new IllegalArgumentException("Eligibility input must be normalized E.164.");
		}
		return required;
	}

	@Override
	public String toString() {
		return "PhoneEligibilityFingerprintHasher[consumerScopeId=" + consumerScopeId
				+ ", keyRegistry=[REDACTED]]";
	}
}
