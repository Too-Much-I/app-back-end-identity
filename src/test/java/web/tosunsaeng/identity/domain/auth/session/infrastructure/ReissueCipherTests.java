package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.domain.RefreshReissueResponse;
import web.tosunsaeng.identity.domain.auth.session.dto.response.ReissueResponse;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

class ReissueCipherTests {
	static final Instant NOW = Instant.parse("2026-09-09T00:00:00Z");
	RefreshSession source;
	ReissueResult result;
	AesGcmReissueResponseCipher cipher;
	Map<String, SecretKey> ring;
	String active;
	@TempDir Path directory;
	@BeforeEach void setup() {
		ring = new HashMap<>(); ring.put("test-v1", new SecretKeySpec(new byte[32], "AES")); active = "test-v1";
		source = RefreshSession.create(UUID.randomUUID().toString(), "test-hash", NOW, NOW.plusSeconds(600));
		source.rotate(NOW, UUID.randomUUID().toString());
		source.recordRecovery("test-request-hash", UUID.randomUUID().toString(), NOW, NOW.plusSeconds(120), UserAccountType.GUEST);
		result = new ReissueResult(new ReissueResponse("test-access", "test-refresh", "Bearer", 300000, 600000),
				NOW.plusSeconds(300), NOW.plusSeconds(600), NOW.plusSeconds(120));
		cipher = cipher("unit-test", 16384);
	}
	AesGcmReissueResponseCipher cipher(String environment, int max) {
		return new AesGcmReissueResponseCipher(new ReissueEncryptionKeyProvider() {
			public String activeKeyId() { return active; }
			public SecretKey key(String id) { var key = ring.get(id); if (key == null) throw SessionSecurityService.unavailable(); return key; }
		}, new ObjectMapper().findAndRegisterModules(), environment, max);
	}
	void unavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(AuthException.class, e -> {
			assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
			assertThat(e.getCause()).isNull(); assertThat(e.getMessage()).doesNotContain("test-access", "test-refresh");
		});
	}
	@Test void roundTripUsesFreshNonceAndNeverStoresPlaintext() throws Exception {
		var a = cipher.encrypt(source, result); var b = cipher.encrypt(source, result);
		assertThat(cipher.decrypt(source, a)).isEqualTo(result); assertThat(a.getNonce()).isNotEqualTo(b.getNonce());
		assertThat(Base64.getDecoder().decode(a.getNonce())).hasSize(12);
		assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(a)).doesNotContain("test-access", "test-refresh");
		assertThat(a.getCleanupAt()).isEqualTo(result.recoveryUntil());
	}
	@ParameterizedTest @ValueSource(strings = {"userId", "sessionId", "replacedBySessionId", "rotationFamilyId", "rotationRequestKeyHash", "rotationResponseId"})
	void sourceIdentityFieldSubstitutionFails(String field) {
		var doc = cipher.encrypt(source, result); ReflectionTestUtils.setField(source, field, UUID.randomUUID().toString());
		unavailable(() -> cipher.decrypt(source, doc));
	}
	@Test void epochTypeAndCommitTimeAreAuthenticated() {
		var doc = cipher.encrypt(source, result); ReflectionTestUtils.setField(source, "sessionEpoch", 1L); unavailable(() -> cipher.decrypt(source, doc));
		ReflectionTestUtils.setField(source, "sessionEpoch", 0L); ReflectionTestUtils.setField(source, "issuedAccountType", UserAccountType.MEMBER); unavailable(() -> cipher.decrypt(source, doc));
		ReflectionTestUtils.setField(source, "issuedAccountType", UserAccountType.GUEST); ReflectionTestUtils.setField(source, "rotationCommittedAt", NOW.minusSeconds(1)); unavailable(() -> cipher.decrypt(source, doc));
	}
	@ParameterizedTest @ValueSource(strings = {"accessExpiresAt", "refreshExpiresAt", "cleanupAt", "recoveryUntil"})
	void documentExpirySubstitutionFails(String field) {
		var doc = cipher.encrypt(source, result); ReflectionTestUtils.setField(doc, field, NOW.plusSeconds(999)); unavailable(() -> cipher.decrypt(source, doc));
	}
	@ParameterizedTest @ValueSource(strings = {"sourceSessionId", "responseId", "encryptionKeyId", "nonce", "ciphertext"})
	void documentSubstitutionFails(String field) {
		var doc = cipher.encrypt(source, result); ReflectionTestUtils.setField(doc, field, "test-tampered"); unavailable(() -> cipher.decrypt(source, doc));
	}
	@Test void validBase64CiphertextBitTamperingFails() {
		var doc = cipher.encrypt(source, result); byte[] bytes = Base64.getDecoder().decode(doc.getCiphertext()); bytes[0] ^= 1;
		ReflectionTestUtils.setField(doc, "ciphertext", Base64.getEncoder().encodeToString(bytes)); unavailable(() -> cipher.decrypt(source, doc));
	}
	@Test void crossEnvironmentReplayIsRejected() { var doc = cipher.encrypt(source, result); unavailable(() -> cipher("other-env", 16384).decrypt(source, doc)); }
	@Test void schemaChangesAreRejected() { var doc = cipher.encrypt(source, result); ReflectionTestUtils.setField(doc, "schemaVersion", 2); unavailable(() -> cipher.decrypt(source, doc)); }
	@Test void oversizedResponseFailsSafely() { unavailable(() -> cipher("unit-test", 10).encrypt(source, result)); }
	@Test void oldKeyDecryptsDuringRotationAndRemovalFailsClosed() {
		var old = cipher.encrypt(source, result); byte[] next = new byte[32]; Arrays.fill(next, (byte) 1);
		ring.put("test-v2", new SecretKeySpec(next, "AES")); active = "test-v2";
		assertThat(cipher.decrypt(source, old)).isEqualTo(result);
		assertThat(cipher.encrypt(source, result).getEncryptionKeyId()).isEqualTo("test-v2");
		ring.remove("test-v1"); unavailable(() -> cipher.decrypt(source, old));
	}
	@Test void mountedKeyringLoadsAndRedacts() throws Exception {
		Path mount = directory.resolve("test-keyring.properties");
		Files.writeString(mount, "test-v1=" + Base64.getEncoder().encodeToString(new byte[32]) + "\n");
		var keys = new MountedReissueEncryptionKeys("test-v1", mount.toString());
		assertThat(keys.key("test-v1").getEncoded()).hasSize(32); assertThat(keys.toString()).isEqualTo("MountedReissueEncryptionKeys[redacted]");
		unavailable(() -> keys.key("unknown"));
	}
	@ParameterizedTest @ValueSource(strings = {"test-v1=invalid-test-data", "test-v1=AA==", "test-v1=AA==\ntest-v1=AA==", "bad key=AA=="})
	void malformedKeyringFailsWithoutEchoingInput(String content) throws Exception {
		Path mount = directory.resolve("bad.properties"); Files.writeString(mount, content);
		assertThatThrownBy(() -> new MountedReissueEncryptionKeys("test-v1", mount.toString()))
				.isInstanceOf(IllegalArgumentException.class).hasMessage("Reissue encryption key configuration is invalid.").hasNoCause();
	}
	@Test void relativeMountAndMissingActiveKeyAreRejected() throws Exception {
		assertThatThrownBy(() -> new MountedReissueEncryptionKeys("test-v1", "relative-test.properties")).isInstanceOf(IllegalArgumentException.class);
		Path mount = directory.resolve("keyring.properties"); Files.writeString(mount, "test-v1=" + Base64.getEncoder().encodeToString(new byte[32]));
		assertThatThrownBy(() -> new MountedReissueEncryptionKeys("test-v2", mount.toString())).isInstanceOf(IllegalArgumentException.class);
	}
}
