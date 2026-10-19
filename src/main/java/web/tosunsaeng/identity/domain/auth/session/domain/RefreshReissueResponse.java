package web.tosunsaeng.identity.domain.auth.session.domain;

import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** Only authenticated ciphertext is stored here; request evidence outlives this document. */
@Getter
@Document("refresh_reissue_responses")
public class RefreshReissueResponse {
	@Id private String responseId;
	@Indexed(name = "uk_reissue_source", unique = true) private String sourceSessionId;
	private int schemaVersion;
	private String encryptionKeyId;
	private String nonce;
	private String ciphertext;
	private Instant accessExpiresAt;
	private Instant refreshExpiresAt;
	private Instant recoveryUntil;
	@Indexed(name = "ttl_reissue_response", expireAfter = "0s") private Instant cleanupAt;
	private RefreshReissueResponse() { }
	public RefreshReissueResponse(String id, String source, String keyId, String nonce, String ciphertext,
			Instant accessExpiry, Instant refreshExpiry, Instant until) {
		this.responseId = id; this.sourceSessionId = source; this.schemaVersion = 1;
		this.encryptionKeyId = keyId; this.nonce = nonce; this.ciphertext = ciphertext;
		this.accessExpiresAt = accessExpiry; this.refreshExpiresAt = refreshExpiry;
		this.recoveryUntil = until; this.cleanupAt = until;
	}
	@Override public String toString() { return "RefreshReissueResponse[redacted]"; }
}
