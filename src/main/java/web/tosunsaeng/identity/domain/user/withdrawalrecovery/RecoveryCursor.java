package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Dedicated recovery key ring; no JWT/support key fallback and no Spring auto-registration. */
public final class RecoveryCursor {
    public static final Duration TTL = Duration.ofHours(1);
    public enum Kind { FEED, SNAPSHOT }
    public record Owner(String environment, String consumer) {
        public Owner { label(environment); label(consumer); }
        @Override public String toString() { return "RecoveryOwner[redacted]"; }
    }
    /** For feed: [start,H2]. For snapshot: [next page ordinal,total page count]. */
    public record Binding(Owner owner, Kind kind, String streamId, String snapshotId, long start, long target, String afterUser) {
        public Binding(Owner owner, Kind kind, String streamId, String snapshotId, long start, long target) {
            this(owner, kind, streamId, snapshotId, start, target, null);
        }
        public Binding {
            if (owner == null || kind == null || start < 0 || target < start) throw invalid();
            RecoveryProtocol.uuid4(streamId);
            if (kind == Kind.SNAPSHOT) { RecoveryProtocol.uuid4(snapshotId); if (target == 0) throw invalid(); }
            else if (snapshotId != null || afterUser != null) throw invalid();
            if (afterUser != null) web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.uuid(afterUser);
        }
        @Override public String toString() { return "RecoveryCursorBinding[redacted]"; }
    }
    public record Verified(Binding binding, Instant issuedAt, Instant expiresAt) {
        @Override public String toString() { return "RecoveryCursorVerified[redacted]"; }
    }
    public static final class Rejected extends RuntimeException {
        private final RecoveryProtocol.Error error;
        private Rejected(RecoveryProtocol.Error error) { super(error.message()); this.error = error; }
        public RecoveryProtocol.Error error() { return error; }
    }
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private final Map<String, byte[]> keys;
    private final String active;
    private final Clock clock;

    public RecoveryCursor(Map<String, byte[]> keyRing, String active, Clock clock) {
        label(active);
        if (keyRing == null || keyRing.isEmpty() || keyRing.size() > 8 || clock == null)
            throw new IllegalArgumentException("Invalid recovery key ring.");
        var copy = new HashMap<String, byte[]>();
        keyRing.forEach((id, key) -> {
            label(id);
            if (key == null || key.length < 32 || key.length > 128)
                throw new IllegalArgumentException("Invalid recovery key size.");
            copy.put(id, key.clone());
        });
        if (!copy.containsKey(active)) throw new IllegalArgumentException("Active recovery key unavailable.");
        this.keys = Map.copyOf(copy); this.active = active; this.clock = clock;
    }

    public String issue(Binding binding) {
        return issueAt(binding, clock.instant());
    }

    /** Continue a snapshot scan without renewing its original expiry. */
    public String advance(Verified prior, Binding next) {
        if (prior == null || prior.binding().kind() != Kind.SNAPSHOT || next.kind() != Kind.SNAPSHOT
                || !prior.binding().owner().equals(next.owner()) || !prior.binding().streamId().equals(next.streamId())
                || !java.util.Objects.equals(prior.binding().snapshotId(), next.snapshotId())
                || prior.binding().target() != next.target() || next.start() != Math.incrementExact(prior.binding().start())) throw invalid();
        if (!clock.instant().isBefore(prior.expiresAt())) throw new Rejected(RecoveryProtocol.Error.CURSOR_EXPIRED);
        return issueAt(next, prior.issuedAt());
    }

    private String issueAt(Binding binding, Instant now) {
        if (binding == null) throw invalid();
        try {
            var bytes = new ByteArrayOutputStream();
            var out = new DataOutputStream(bytes);
            out.writeUTF(binding.owner().environment()); out.writeUTF(binding.owner().consumer());
            out.writeUTF(binding.kind().name()); out.writeUTF(binding.streamId());
            out.writeUTF(binding.snapshotId() == null ? "" : binding.snapshotId());
            out.writeLong(binding.start()); out.writeLong(binding.target());
            out.writeLong(now.getEpochSecond()); out.writeInt(now.getNano());
            out.writeUTF(binding.afterUser() == null ? "" : binding.afterUser());
            String signed = "r1." + active + "." + ENCODER.encodeToString(bytes.toByteArray());
            String token = signed + "." + ENCODER.encodeToString(mac(keys.get(active), signed));
            if (token.length() > RecoveryProtocol.MAX_CURSOR_CHARS) throw invalid();
            return token;
        } catch (IOException impossible) { throw invalid(); }
    }

    public Verified verify(String token, Owner owner, Kind kind, String streamId, String snapshotId) {
        if (token == null || token.length() > RecoveryProtocol.MAX_CURSOR_CHARS) throw invalid();
        String[] parts = token.split("\\.", -1);
        if (parts.length != 4 || !parts[0].equals("r1") || !keys.containsKey(parts[1])) throw invalid();
        byte[] signature = decode(parts[3]);
        String signed = parts[0] + "." + parts[1] + "." + parts[2];
        if (signature.length != 32 || !MessageDigest.isEqual(signature, mac(keys.get(parts[1]), signed)))
            throw invalid();
        try {
            var in = new DataInputStream(new ByteArrayInputStream(decode(parts[2])));
            Owner storedOwner = new Owner(in.readUTF(), in.readUTF());
            Kind storedKind = Kind.valueOf(in.readUTF());
            String stream = in.readUTF(); String snapshot = in.readUTF();
            long start = in.readLong(); long target = in.readLong();
            long second = in.readLong(); int nano = in.readInt();
            String afterUser = in.available() == 0 ? "" : in.readUTF();
            var binding = new Binding(storedOwner, storedKind, stream, snapshot.isEmpty() ? null : snapshot,
                    start, target, afterUser.isEmpty() ? null : afterUser);
            if (nano < 0 || nano > 999_999_999 || in.available() != 0) throw invalid();
            Instant issued = Instant.ofEpochSecond(second, nano);
            if (!binding.owner().equals(owner) || binding.kind() != kind || !binding.streamId().equals(streamId)
                    || !java.util.Objects.equals(binding.snapshotId(), snapshotId)) throw invalid();
            Instant now = clock.instant(); Instant expiry = issued.plus(TTL);
            if (issued.isAfter(now)) throw invalid();
            if (!now.isBefore(expiry)) throw new Rejected(RecoveryProtocol.Error.CURSOR_EXPIRED);
            return new Verified(binding, issued, expiry);
        } catch (Rejected known) { throw known; }
        catch (IOException | IllegalArgumentException | java.time.DateTimeException failure) { throw invalid(); }
    }

    public static void requireFeedPosition(Verified cursor, long after) {
        if (cursor == null || cursor.binding().kind() != Kind.FEED
                || after < cursor.binding().start() || after > cursor.binding().target()) throw invalid();
    }
    private static byte[] decode(String text) {
        try {
            if (text.isEmpty() || !text.matches("[A-Za-z0-9_-]+")) throw invalid();
            byte[] bytes = DECODER.decode(text);
            if (!ENCODER.encodeToString(bytes).equals(text)) throw invalid();
            return bytes;
        } catch (IllegalArgumentException failure) { throw invalid(); }
    }
    private static byte[] mac(byte[] key, String text) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(text.getBytes(StandardCharsets.US_ASCII));
        } catch (java.security.GeneralSecurityException failure) {
            throw new IllegalStateException("Recovery signing unavailable.");
        }
    }
    private static void label(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) throw invalid();
    }
    private static Rejected invalid() { return new Rejected(RecoveryProtocol.Error.INVALID_REQUEST); }
    @Override public String toString() { return "RecoveryCursor[redacted]"; }
}
