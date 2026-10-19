package web.tosunsaeng.identity.global.security.jwt;

import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.security.Key;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.RSAKey;

import com.nimbusds.jose.jwk.JWKSet;

public final class JwksPublicKeySet {

	private final JWKSet value;

	public JwksPublicKeySet(JWKSet value) {
		this.value = Objects.requireNonNull(value).toPublicJWKSet();
	}

	/** Select only a configured public key. Never resolve token-supplied key URLs. */
	public List<? extends Key> verificationKeys(JWSHeader header) throws KeySourceException {
		String kid = header.getKeyID();
		if (!JWSAlgorithm.RS256.equals(header.getAlgorithm()) || kid == null || kid.isBlank()) {
			return List.of();
		}
		var key = value.getKeyByKeyId(kid);
		try {
			return key instanceof RSAKey rsa ? List.of(rsa.toRSAPublicKey()) : List.of();
		} catch (JOSEException exception) {
			throw new KeySourceException("Configured verification key is invalid.");
		}
	}

	public Map<String, Object> toJsonObject() {
		return value.toJSONObject();
	}

	@Override
	public String toString() {
		return "JwksPublicKeySet[REDACTED]";
	}
}
