package web.tosunsaeng.identity.domain.auth.session.infrastructure;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.application.*;
import web.tosunsaeng.identity.domain.auth.session.domain.RefreshReissueResponse;

public final class AesGcmReissueResponseCipher implements ReissueResponseCipher {
	private final ReissueEncryptionKeyProvider keys;
	private final ObjectMapper json;
	private final String environment;
	private final int maxBytes;
	private final SecureRandom random = new SecureRandom();
	public AesGcmReissueResponseCipher(ReissueEncryptionKeyProvider keys, ObjectMapper json, String environment, int maxBytes) {
		this.keys = Objects.requireNonNull(keys); this.json = json.copy();
		this.environment = Objects.requireNonNull(environment); this.maxBytes = maxBytes;
	}
	@Override public RefreshReissueResponse encrypt(RefreshSession source, ReissueResult result) {
		byte[] plain = null;
		try {
			plain = json.writeValueAsBytes(result);
			if (plain.length > maxBytes) throw new IllegalArgumentException();
			byte[] nonce = new byte[12]; random.nextBytes(nonce);
			String keyId = keys.activeKeyId();
			var metadata = new RefreshReissueResponse(source.getRotationResponseId(), source.getSessionId(), keyId,
					Base64.getEncoder().encodeToString(nonce), "", result.accessExpiresAt(), result.refreshExpiresAt(), result.recoveryUntil());
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, keys.key(keyId), new GCMParameterSpec(128, nonce));
			cipher.updateAAD(aad(source, metadata));
			return new RefreshReissueResponse(metadata.getResponseId(), metadata.getSourceSessionId(), keyId,
					metadata.getNonce(), Base64.getEncoder().encodeToString(cipher.doFinal(plain)),
					result.accessExpiresAt(), result.refreshExpiresAt(), result.recoveryUntil());
		} catch (Exception exception) { throw SessionSecurityService.unavailable(); }
		finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
	}
	@Override public ReissueResult decrypt(RefreshSession source, RefreshReissueResponse document) {
		byte[] plain = null;
		try {
			if (document.getSchemaVersion() != 1 || !source.getRotationResponseId().equals(document.getResponseId())
					|| !source.getSessionId().equals(document.getSourceSessionId())
					|| !source.getRecoveryUntil().equals(document.getRecoveryUntil())
					|| !document.getRecoveryUntil().equals(document.getCleanupAt())
					|| document.getNonce().length() != 16 || document.getCiphertext().length() > (maxBytes + 18) * 4 / 3 + 4) {
				throw new IllegalArgumentException();
			}
			byte[] nonce = Base64.getDecoder().decode(document.getNonce());
			if (nonce.length != 12) throw new IllegalArgumentException();
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, keys.key(document.getEncryptionKeyId()), new GCMParameterSpec(128, nonce));
			cipher.updateAAD(aad(source, document));
			plain = cipher.doFinal(Base64.getDecoder().decode(document.getCiphertext()));
			if (plain.length > maxBytes) throw new IllegalArgumentException();
			ReissueResult result = json.readValue(plain, ReissueResult.class);
			if (!document.getAccessExpiresAt().equals(result.accessExpiresAt())
					|| !document.getRefreshExpiresAt().equals(result.refreshExpiresAt())
					|| !document.getRecoveryUntil().equals(result.recoveryUntil())) throw new IllegalArgumentException();
			return result;
		} catch (Exception exception) { throw SessionSecurityService.unavailable(); }
		finally { if (plain != null) Arrays.fill(plain, (byte) 0); }
	}
	private byte[] aad(RefreshSession source, RefreshReissueResponse document) throws Exception {
		// JSON array encoding preserves field boundaries. All inputs are server-owned snapshots.
		return json.writeValueAsBytes(List.of("identity/reissue", environment, document.getSchemaVersion(),
				document.getEncryptionKeyId(), source.getRotationResponseId(), source.getSessionId(),
				source.getReplacedBySessionId(), source.getUserId(), source.getRotationFamilyId(),
				source.getRotationRequestKeyHash(), source.getSessionEpoch(), source.getIssuedAccountType(),
				source.getRotationCommittedAt(), source.getRecoveryUntil(), document.getAccessExpiresAt(), document.getRefreshExpiresAt()));
	}
}
