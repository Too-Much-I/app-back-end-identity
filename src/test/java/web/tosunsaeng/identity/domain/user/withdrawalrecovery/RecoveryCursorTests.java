package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RecoveryCursorTests {
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00.123456789Z");
    private static final String STREAM = "00000000-0000-4000-8000-000000000001";
    private static final String RUN = "00000000-0000-4000-8000-000000000002";
    private static final RecoveryCursor.Owner OWNER = new RecoveryCursor.Owner("test", "billing");
    private static byte[] key(int value) { byte[] key = new byte[32]; Arrays.fill(key, (byte) value); return key; }
    private RecoveryCursor codec(Instant now) { return new RecoveryCursor(Map.of("key1", key(1)), "key1", Clock.fixed(now, ZoneOffset.UTC)); }
    private RecoveryCursor.Binding feed() { return new RecoveryCursor.Binding(OWNER, RecoveryCursor.Kind.FEED, STREAM, null, 1, Long.MAX_VALUE); }
    private RecoveryCursor.Verified verify(RecoveryCursor codec, String token) { return codec.verify(token, OWNER, RecoveryCursor.Kind.FEED, STREAM, null); }

    @Test void roundTripAndPositionBoundsPreserveMaxAndNanoseconds() {
        var codec = codec(NOW); String token = codec.issue(feed()); var verified = verify(codec, token);
        assertThat(verified.binding()).isEqualTo(feed());
        assertThat(verified.issuedAt()).isEqualTo(NOW);
        assertThat(verified.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        RecoveryCursor.requireFeedPosition(verified, Long.MAX_VALUE);
        assertThatThrownBy(() -> RecoveryCursor.requireFeedPosition(verified, 0)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThat(token.length()).isLessThanOrEqualTo(2048);
    }
    @Test void rejectsExpiryExactlyAndFutureIssued() {
        String token = codec(NOW).issue(feed());
        assertThatThrownBy(() -> verify(codec(NOW.plusSeconds(3600)), token)).isInstanceOfSatisfying(RecoveryCursor.Rejected.class,
                e -> assertThat(e.error()).isEqualTo(RecoveryProtocol.Error.CURSOR_EXPIRED));
        verify(codec(NOW.plusSeconds(3600).minusNanos(1)), token);
        assertThatThrownBy(() -> verify(codec(NOW.minusNanos(1)), token)).isInstanceOf(RecoveryCursor.Rejected.class);
    }
    @Test void rejectsOtherOwnerEnvironmentPurposeStreamAndRun() {
        var codec = codec(NOW); String token = codec.issue(feed());
        assertThatThrownBy(() -> codec.verify(token, new RecoveryCursor.Owner("prod", "billing"), RecoveryCursor.Kind.FEED, STREAM, null)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> codec.verify(token, new RecoveryCursor.Owner("test", "other"), RecoveryCursor.Kind.FEED, STREAM, null)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> codec.verify(token, OWNER, RecoveryCursor.Kind.FEED, RUN, null)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> codec.verify(token, OWNER, RecoveryCursor.Kind.SNAPSHOT, STREAM, RUN)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> codec.verify(token, OWNER, RecoveryCursor.Kind.FEED, STREAM, RUN)).isInstanceOf(RecoveryCursor.Rejected.class);
    }
    @Test void rotationAcceptsPreviousOnlyWhileExplicitlyInRingAndCopiesKeyMaterial() {
        byte[] old = key(1); var issuer = new RecoveryCursor(Map.of("old", old), "old", Clock.fixed(NOW, ZoneOffset.UTC));
        String token = issuer.issue(feed()); Arrays.fill(old, (byte) 0);
        var rotated = new RecoveryCursor(Map.of("old", key(1), "new", key(2)), "new", Clock.fixed(NOW, ZoneOffset.UTC));
        verify(rotated, token); verify(issuer, token);
        assertThatThrownBy(() -> verify(new RecoveryCursor(Map.of("new", key(2)), "new", Clock.fixed(NOW, ZoneOffset.UTC)), token)).isInstanceOf(RecoveryCursor.Rejected.class);
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "r1.a.b.c", "r1.key1.e30.invalid", "r1.key1.e30=.invalid", "\n"})
    void malformedNeverLeaksPayload(String token) {
        assertThatThrownBy(() -> verify(codec(NOW), token)).isInstanceOf(RecoveryCursor.Rejected.class).hasMessage("Invalid recovery request.");
    }
    @Test void detectsSignedPartOrSignatureMutationAndOverLimit() {
        var codec = codec(NOW); String token = codec.issue(feed());
        int at = token.lastIndexOf('.') + 1;
        String changed = token.substring(0, at) + (token.charAt(at) == 'A' ? 'B' : 'A') + token.substring(at + 1);
        assertThatThrownBy(() -> verify(codec, changed)).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> verify(codec, token.replace("r1.", "r2."))).isInstanceOf(RecoveryCursor.Rejected.class);
        assertThatThrownBy(() -> verify(codec, "a".repeat(2049))).isInstanceOf(RecoveryCursor.Rejected.class);
    }
    @Test void snapshotBindingAndRedaction() {
        var codec = codec(NOW);
        var binding = new RecoveryCursor.Binding(OWNER, RecoveryCursor.Kind.SNAPSHOT, STREAM, RUN, 2, 5);
        var result = codec.verify(codec.issue(binding), OWNER, RecoveryCursor.Kind.SNAPSHOT, STREAM, RUN);
        assertThat(result.binding()).isEqualTo(binding);
        assertThat(result.toString() + codec + binding + OWNER).doesNotContain(STREAM, RUN, "billing", "key1");
        assertThatThrownBy(() -> RecoveryCursor.requireFeedPosition(result, 2)).isInstanceOf(RecoveryCursor.Rejected.class);
    }
    @Test void invalidKeyRingFailsClosed() {
        assertThatThrownBy(() -> new RecoveryCursor(Map.of("key1", new byte[16]), "key1", Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecoveryCursor(Map.of("key1", key(1)), "missing", Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
    }
}
