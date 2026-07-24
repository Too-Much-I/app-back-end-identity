package web.tosunsaeng.identity.security.jwt;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;

public record RsaKeyMaterial(
		RSAPrivateKey privateKey,
		RSAPublicKey publicKey
) {

	public RsaKeyMaterial {
		Objects.requireNonNull(privateKey, "privateKey must not be null");
		Objects.requireNonNull(publicKey, "publicKey must not be null");

		if (!privateKey.getModulus().equals(publicKey.getModulus())) {
			throw new IllegalArgumentException("Configured RSA keys do not form a key pair.");
		}
	}

	@Override
	public String toString() {
		return "RsaKeyMaterial[privateKey=redacted, publicKey=RSA]";
	}
}
