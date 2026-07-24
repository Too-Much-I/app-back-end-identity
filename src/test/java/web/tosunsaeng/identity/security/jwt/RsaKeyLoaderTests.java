package web.tosunsaeng.identity.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;

class RsaKeyLoaderTests {

	private static final String PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
	private static final String PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
	private static final String PUBLIC_HEADER = "-----BEGIN PUBLIC KEY-----";
	private static final String PUBLIC_FOOTER = "-----END PUBLIC KEY-----";

	@TempDir
	private Path temporaryDirectory;

	private RsaKeyLoader rsaKeyLoader;

	@BeforeEach
	void setUp() {
		rsaKeyLoader = new RsaKeyLoader(new DefaultResourceLoader());
	}

	@Test
	void readsPkcs8PrivateKeyAndX509PublicKey() throws IOException {
		KeyPair keyPair = generateKeyPair("RSA", 2048);
		Path privateKeyPath = temporaryDirectory.resolve("private.pem");
		Path publicKeyPath = temporaryDirectory.resolve("public.pem");
		Files.writeString(privateKeyPath, pem(PRIVATE_HEADER, keyPair.getPrivate().getEncoded(), PRIVATE_FOOTER));
		Files.writeString(publicKeyPath, pem(PUBLIC_HEADER, keyPair.getPublic().getEncoded(), PUBLIC_FOOTER));

		RsaKeyMaterial keyMaterial = rsaKeyLoader.load(
				privateKeyPath.toUri().toString(),
				publicKeyPath.toUri().toString()
		);
		RSAPrivateKey privateKey = keyMaterial.privateKey();
		RSAPublicKey publicKey = keyMaterial.publicKey();

		assertThat(privateKey.getModulus()).isEqualTo(((RSAPrivateKey) keyPair.getPrivate()).getModulus());
		assertThat(publicKey.getModulus()).isEqualTo(((RSAPublicKey) keyPair.getPublic()).getModulus());
		assertThat(keyMaterial.toString())
				.isEqualTo("RsaKeyMaterial[privateKey=redacted, publicKey=RSA]")
				.doesNotContain(privateKey.toString());
	}

	@Test
	void rejectsInvalidKeyWithSafeErrorThatDoesNotExposeFileContents() throws IOException {
		String sensitiveFileContents = "invalid-key-material-that-must-not-be-exposed";
		Path invalidKeyPath = temporaryDirectory.resolve("invalid.pem");
		Files.writeString(invalidKeyPath, sensitiveFileContents);

		assertThatThrownBy(() -> rsaKeyLoader.loadPrivateKey(new FileSystemResource(invalidKeyPath)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("RSA private key could not be loaded from the configured resource.")
				.hasMessageNotContaining(sensitiveFileContents)
				.hasNoCause();
	}

	@Test
	void rejectsNonRsaPkcs8Key() throws IOException {
		KeyPair ecKeyPair = generateKeyPair("EC", 256);
		Path privateKeyPath = temporaryDirectory.resolve("ec-private.pem");
		Files.writeString(
				privateKeyPath,
				pem(PRIVATE_HEADER, ecKeyPair.getPrivate().getEncoded(), PRIVATE_FOOTER)
		);

		assertThatThrownBy(() -> rsaKeyLoader.loadPrivateKey(new FileSystemResource(privateKeyPath)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("RSA private key could not be loaded from the configured resource.");
	}

	@Test
	void rejectsPrivateAndPublicKeysThatDoNotFormPair() {
		KeyPair firstPair = generateKeyPair("RSA", 2048);
		KeyPair secondPair = generateKeyPair("RSA", 2048);

		assertThatThrownBy(() -> new RsaKeyMaterial(
				(RSAPrivateKey) firstPair.getPrivate(),
				(RSAPublicKey) secondPair.getPublic()
		))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Configured RSA keys do not form a key pair.");
	}

	private KeyPair generateKeyPair(String algorithm, int keySize) {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
			generator.initialize(keySize);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(algorithm + " is not available in the test runtime.");
		}
	}

	private String pem(String header, byte[] encoded, String footer) {
		String payload = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
		return header + "\n" + payload + "\n" + footer + "\n";
	}
}
