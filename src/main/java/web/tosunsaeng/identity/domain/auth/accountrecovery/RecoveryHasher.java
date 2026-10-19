package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Independent key ring. Retain every key across all live proof/retry/rate-limit windows. */
public final class RecoveryHasher {
	private final Map<String, byte[]> keys = new LinkedHashMap<>();
	public RecoveryHasher(String ring) {
		try {
			for (String entry : Objects.requireNonNull(ring).split(",")) {
				String[] parts = entry.trim().split(":", 2);
				if (parts.length != 2 || !parts[0].matches("[a-zA-Z0-9_-]{1,32}")) throw new IllegalArgumentException();
				byte[] value = Base64.getDecoder().decode(parts[1]);
				if (value.length < 32 || keys.putIfAbsent(parts[0], value) != null) throw new IllegalArgumentException();
			}
			if (keys.isEmpty() || keys.size() > 4) throw new IllegalArgumentException();
		} catch (RuntimeException e) { throw new IllegalArgumentException("Invalid account recovery key ring"); }
	}
	public List<String> hashes(String purpose, String... values) {
		var result = new ArrayList<String>();
		for (var entry : keys.entrySet()) {
			try {
				Mac mac = Mac.getInstance("HmacSHA256");
				mac.init(new SecretKeySpec(entry.getValue(), "HmacSHA256"));
				for (String value : concat(purpose, values)) {
					byte[] b = value.getBytes(StandardCharsets.UTF_8);
					mac.update(java.nio.ByteBuffer.allocate(4).putInt(b.length).array()); mac.update(b);
				}
				result.add(entry.getKey() + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal()));
			} catch (java.security.GeneralSecurityException e) { throw new IllegalStateException("Recovery hashing unavailable"); }
		}
		return List.copyOf(result);
	}
	private List<String> concat(String purpose, String[] values) {
		var result = new ArrayList<String>(); result.add("identity:account-recovery:v1"); result.add(purpose);
		result.addAll(Arrays.asList(values)); return result;
	}
	@Override public String toString() { return "RecoveryHasher[REDACTED]"; }
}
