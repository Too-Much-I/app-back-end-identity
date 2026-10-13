package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords;

/** Streaming canonical digest; independent of public Jackson configuration and page boundaries. */
public final class SnapshotDigest {
    private final MessageDigest digest;
    private String previousUserId;
    private long itemCount;
    private String sealed;

    public record Item(String userId, Instant withdrawnAt) {
        public Item {
            WithdrawalRecords.uuid(userId);
            Objects.requireNonNull(withdrawnAt, "Withdrawal time required.");
        }
        @Override public String toString() { return "SnapshotItem[redacted]"; }
    }

    public SnapshotDigest() {
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable."); }
    }

    public void append(Item item) {
        if (sealed != null) throw new IllegalStateException("Snapshot digest already sealed.");
        if (previousUserId != null && previousUserId.compareTo(item.userId()) >= 0)
            throw new IllegalArgumentException("Snapshot items must be strictly ordered.");
        long nextCount = Math.incrementExact(itemCount);
        // UUID and canonical Instant alphabets cannot contain JSON escapes. Preserve nanoseconds.
        byte[] bytes = ("{\"userId\":\"" + item.userId() + "\",\"withdrawnAt\":\""
                + item.withdrawnAt() + "\"}").getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
        previousUserId = item.userId();
        itemCount = nextCount;
    }

    public long itemCount() { return itemCount; }

    public String finish() {
        if (sealed == null) sealed = HexFormat.of().formatHex(digest.digest());
        return sealed;
    }
}
