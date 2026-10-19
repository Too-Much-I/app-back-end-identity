package web.tosunsaeng.identity.domain.auth.session.application;

import javax.crypto.SecretKey;

/** Loaded locally before transactions. Never use JWT or phone fingerprint keys. */
public interface ReissueEncryptionKeyProvider {
	String activeKeyId();
	SecretKey key(String id);
}
