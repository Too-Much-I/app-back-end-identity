package web.tosunsaeng.identity.global.security.jwt;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt-rotation")
public class JwksRotationProperties {

	private String previousKeyIds = "";
	private String previousPublicKeyLocations = "";

	public List<String> keyIds() {
		return split(previousKeyIds);
	}

	public List<String> publicKeyLocations() {
		return split(previousPublicKeyLocations);
	}

	public void validate(String activeKeyId) {
		List<String> keyIds = keyIds();
		List<String> locations = publicKeyLocations();
		if (keyIds.size() != locations.size()
				|| keyIds.stream().distinct().count() != keyIds.size()
				|| keyIds.contains(activeKeyId)) {
			throw new IllegalArgumentException("JWKS rotation configuration is invalid.");
		}
	}

	private static List<String> split(String value) {
		if (value == null || value.isBlank()) {
			return List.of();
		}
		List<String> values = Arrays.stream(value.split(",", -1))
				.map(String::trim)
				.toList();
		if (values.stream().anyMatch(String::isBlank)) {
			throw new IllegalArgumentException("JWKS rotation configuration is invalid.");
		}
		return values;
	}

	public String getPreviousKeyIds() { return previousKeyIds; }
	public void setPreviousKeyIds(String value) { this.previousKeyIds = value == null ? "" : value; }
	public String getPreviousPublicKeyLocations() { return previousPublicKeyLocations; }
	public void setPreviousPublicKeyLocations(String value) {
		this.previousPublicKeyLocations = value == null ? "" : value;
	}

	@Override
	public String toString() {
		return "JwksRotationProperties[previousKeyCount=" + keyIds().size()
				+ ", locations=[REDACTED]]";
	}
}
