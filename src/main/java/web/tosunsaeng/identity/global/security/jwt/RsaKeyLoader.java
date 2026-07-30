package web.tosunsaeng.identity.global.security.jwt;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

public class RsaKeyLoader {

	private static final String PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
	private static final String PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";
	private static final String PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
	private static final String PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
	private static final String PRIVATE_KEY_ERROR =
			"RSA private key could not be loaded from the configured resource.";
	private static final String PUBLIC_KEY_ERROR =
			"RSA public key could not be loaded from the configured resource.";

	private final ResourceLoader resourceLoader;

	public RsaKeyLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	public RsaKeyMaterial load(String privateKeyLocation, String publicKeyLocation) {
		RSAPrivateKey privateKey = loadPrivateKey(resourceLoader.getResource(privateKeyLocation));
		RSAPublicKey publicKey = loadPublicKey(resourceLoader.getResource(publicKeyLocation));
		return new RsaKeyMaterial(privateKey, publicKey);
	}

	RSAPrivateKey loadPrivateKey(Resource resource) {
		byte[] encoded = null;
		try {
			encoded = readPem(resource, PRIVATE_KEY_HEADER, PRIVATE_KEY_FOOTER);
			Key key = KeyFactory.getInstance("RSA")
					.generatePrivate(new PKCS8EncodedKeySpec(encoded));
			if (!(key instanceof RSAPrivateKey rsaPrivateKey)) {
				throw new GeneralSecurityException("Loaded private key is not RSA.");
			}
			return rsaPrivateKey;
		} catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException(PRIVATE_KEY_ERROR);
		} finally {
			if (encoded != null) {
				Arrays.fill(encoded, (byte) 0);
			}
		}
	}

	RSAPublicKey loadPublicKey(Resource resource) {
		byte[] encoded = null;
		try {
			encoded = readPem(resource, PUBLIC_KEY_HEADER, PUBLIC_KEY_FOOTER);
			Key key = KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(encoded));
			if (!(key instanceof RSAPublicKey rsaPublicKey)) {
				throw new GeneralSecurityException("Loaded public key is not RSA.");
			}
			return rsaPublicKey;
		} catch (IOException | GeneralSecurityException | IllegalArgumentException exception) {
			throw new IllegalStateException(PUBLIC_KEY_ERROR);
		} finally {
			if (encoded != null) {
				Arrays.fill(encoded, (byte) 0);
			}
		}
	}

	private byte[] readPem(Resource resource, String header, String footer) throws IOException {
		try (InputStream inputStream = resource.getInputStream()) {
			String pem = new String(inputStream.readAllBytes(), StandardCharsets.US_ASCII).trim();
			if (!pem.startsWith(header) || !pem.endsWith(footer)) {
				throw new IllegalArgumentException("Unsupported PEM format.");
			}

			String payload = pem.substring(header.length(), pem.length() - footer.length())
					.replaceAll("\\s", "");
			if (payload.isEmpty()) {
				throw new IllegalArgumentException("Empty PEM payload.");
			}
			return Base64.getDecoder().decode(payload);
		}
	}
}
