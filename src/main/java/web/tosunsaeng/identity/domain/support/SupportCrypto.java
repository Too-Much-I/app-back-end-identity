package web.tosunsaeng.identity.domain.support;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class SupportCrypto {
    private final byte[] key;
    public SupportCrypto(String encoded) {
        try { key = Base64.getDecoder().decode(encoded == null ? "" : encoded); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Support HMAC key must be base64"); }
        if (key.length < 32) throw new IllegalArgumentException("Support HMAC key must contain at least 32 bytes");
    }
    public String hash(String domain, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((domain + "\0" + value).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) { throw SupportError.SUPPORT_INQUIRY_UNAVAILABLE.exception(); }
    }
    @Override public String toString() { return "SupportCrypto[REDACTED]"; }
}
