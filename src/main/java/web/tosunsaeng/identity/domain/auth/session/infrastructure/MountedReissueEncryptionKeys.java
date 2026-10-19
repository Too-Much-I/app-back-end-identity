package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import web.tosunsaeng.identity.domain.auth.session.application.ReissueEncryptionKeyProvider;
import web.tosunsaeng.identity.domain.auth.session.application.SessionSecurityService;

/** Read-only secret mount, supplied by the deployment platform. No network or secret logging. */
public final class MountedReissueEncryptionKeys implements ReissueEncryptionKeyProvider {
	private final String active;
	private final Map<String, SecretKey> keys;
	public MountedReissueEncryptionKeys(String active, String location) {
		byte[] input = null;
		try {
			if (active == null || !active.matches("[a-zA-Z0-9_-]{1,64}")) throw new IllegalArgumentException();
			Path path = Path.of(location);
			if (!path.isAbsolute() || !Files.isRegularFile(path)) throw new IllegalArgumentException();
			try (var stream = Files.newInputStream(path)) { input = stream.readNBytes(16385); }
			if (input.length > 16384) throw new IllegalArgumentException();
			Properties values = new Properties() {
				@Override public synchronized Object put(Object key, Object value) {
					if (containsKey(key)) throw new IllegalArgumentException();
					return super.put(key, value);
				}
			};
			values.load(new ByteArrayInputStream(input));
			if (values.isEmpty() || values.size() > 8) throw new IllegalArgumentException();
			Map<String, SecretKey> loaded = new HashMap<>();
			for (String id : values.stringPropertyNames()) {
				if (!id.matches("[a-zA-Z0-9_-]{1,64}")) throw new IllegalArgumentException();
				byte[] raw = Base64.getDecoder().decode(values.getProperty(id));
				try {
					if (raw.length != 32) throw new IllegalArgumentException();
					loaded.put(id, new SecretKeySpec(raw, "AES"));
				} finally { Arrays.fill(raw, (byte) 0); }
			}
			if (!loaded.containsKey(active)) throw new IllegalArgumentException();
			this.active = active; this.keys = Map.copyOf(loaded);
		} catch (Exception exception) {
			// Do not retain parser exceptions which may include key material or mount paths.
			throw new IllegalArgumentException("Reissue encryption key configuration is invalid.");
		} finally { if (input != null) Arrays.fill(input, (byte) 0); }
	}
	public String activeKeyId() { return active; }
	public SecretKey key(String id) {
		SecretKey key = keys.get(id);
		if (key == null) throw SessionSecurityService.unavailable();
		return key;
	}
	@Override public String toString() { return "MountedReissueEncryptionKeys[redacted]"; }
}
