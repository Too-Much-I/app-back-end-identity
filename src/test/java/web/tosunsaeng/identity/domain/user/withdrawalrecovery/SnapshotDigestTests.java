package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SnapshotDigestTests {
    @Test void productionDigestMatchesUnmodifiedTmi199GoldenIncludingNanoseconds() throws Exception {
        try (var input = getClass().getResourceAsStream("/contracts/payment-lifecycle/v1/snapshot-digest.json")) {
            var fixture = new ObjectMapper().readTree(input);
            var digest = new SnapshotDigest();
            for (var row : fixture.get("canonicalRows")) {
                digest.append(new SnapshotDigest.Item(row.get("userId").asText(),
                        Instant.parse(row.get("withdrawnAt").asText())));
            }
            assertThat(digest.finish()).isEqualTo(fixture.get("expectedDigest").asText());
            assertThat(digest.itemCount()).isEqualTo(4);
            assertThat(digest.finish()).isEqualTo(fixture.get("expectedDigest").asText());
            assertThat(new SnapshotDigest().finish()).isEqualTo(fixture.get("expectedEmptyDigest").asText());
        }
    }

    @Test void rejectsDuplicatesOutOfOrderAndAppendAfterSeal() {
        var one = new SnapshotDigest.Item("11111111-1111-4111-8111-111111111111", Instant.EPOCH);
        var two = new SnapshotDigest.Item("22222222-2222-4222-8222-222222222222", Instant.EPOCH);
        var duplicate = new SnapshotDigest();
        duplicate.append(one);
        assertThatThrownBy(() -> duplicate.append(one)).isInstanceOf(IllegalArgumentException.class);
        var reversed = new SnapshotDigest();
        reversed.append(two);
        assertThatThrownBy(() -> reversed.append(one)).isInstanceOf(IllegalArgumentException.class);
        duplicate.finish();
        assertThatThrownBy(() -> duplicate.append(two)).isInstanceOf(IllegalStateException.class);
        assertThat(one.toString()).doesNotContain(one.userId());
    }

    @Test void rejectsInvalidIdentifiersButDoesNotRequireUserUuidV4() {
        assertThatThrownBy(() -> new SnapshotDigest.Item("not-a-uuid", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnapshotDigest.Item("AAAAAAAA-AAAA-4AAA-8AAA-AAAAAAAAAAAA", Instant.EPOCH))
                .isInstanceOf(IllegalArgumentException.class);
        var item = new SnapshotDigest.Item("11111111-1111-1111-8111-111111111111", Instant.EPOCH);
        var digest = new SnapshotDigest();
        digest.append(item);
        assertThat(digest.itemCount()).isEqualTo(1);
    }
}
