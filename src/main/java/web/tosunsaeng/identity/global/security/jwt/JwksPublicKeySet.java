package web.tosunsaeng.identity.global.security.jwt;

import java.util.Map;
import java.util.Objects;

import com.nimbusds.jose.jwk.JWKSet;

public final class JwksPublicKeySet {

	private final JWKSet value;

	public JwksPublicKeySet(JWKSet value) {
		this.value = Objects.requireNonNull(value);
	}

	public Map<String, Object> toJsonObject() {
		return value.toJSONObject();
	}

	@Override
	public String toString() {
		return "JwksPublicKeySet[REDACTED]";
	}
}
