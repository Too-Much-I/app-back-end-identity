package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.UnavailableRanges.*;

class UnavailableRangesTests {
    @Test void neverBridgesAvailableSequences() {
        assertThat(normalize(100, 110, List.of(new Range(105, 108))))
                .isEqualTo(new Normalized(100, List.of(new Range(105, 108))));
        assertThat(normalize(100, 110, List.of(new Range(105, 108), new Range(101, 104))))
                .isEqualTo(new Normalized(108, List.of()));
    }

    @Test void mergesAdjacentOverlappingAndDuplicateEvidence() {
        assertThat(normalize(2, 20, List.of(new Range(10, 12), new Range(8, 10),
                new Range(13, 14), new Range(8, 9), new Range(16, 18))))
                .isEqualTo(new Normalized(2, List.of(new Range(8, 14), new Range(16, 18))));
    }

    @Test void safelyHandlesMaximumAndRedundantPrefix() {
        assertThat(normalize(Long.MAX_VALUE - 1, Long.MAX_VALUE,
                List.of(new Range(Long.MAX_VALUE, Long.MAX_VALUE))))
                .isEqualTo(new Normalized(Long.MAX_VALUE, List.of()));
        assertThat(normalize(Long.MAX_VALUE, Long.MAX_VALUE, List.of(new Range(1, Long.MAX_VALUE))))
                .isEqualTo(new Normalized(Long.MAX_VALUE, List.of()));
        assertThat(normalize(0, Long.MAX_VALUE, List.of(new Range(Long.MAX_VALUE - 1, Long.MAX_VALUE))))
                .isEqualTo(new Normalized(0, List.of(new Range(Long.MAX_VALUE - 1, Long.MAX_VALUE))));
    }

    @Test void rejectsUnboundedOrContradictoryInputs() {
        assertThatThrownBy(() -> new Range(0, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Range(2, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalize(-1, 10, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalize(2, 1, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalize(0, 10, List.of(new Range(8, 11))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> normalize(0, 10, java.util.Collections.nCopies(101, new Range(1, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void partialBatchLeavesUnselectedEvidenceForTheNextBatch() {
        var first = normalize(0, 5, List.of(new Range(1, 2)));
        var second = normalize(first.replayFloor(), 5, List.of(new Range(3, 5)));
        assertThat(second).isEqualTo(normalize(0, 5, List.of(new Range(1, 2), new Range(3, 5))));
    }

    @Test void deterministicPropertyCheckPreservesExactlyTheUnavailableSet() {
        var random = new Random(203);
        for (int trial = 0; trial < 2000; trial++) {
            int floor = random.nextInt(21);
            var source = new ArrayList<Range>();
            for (int i = random.nextInt(30); i > 0; i--) {
                int from = 1 + random.nextInt(50);
                source.add(new Range(from, from + random.nextInt(51 - from)));
            }
            var result = normalize(floor, 50, source);
            for (long sequence = 1; sequence <= 50; sequence++) {
                long seq = sequence;
                boolean before = seq <= floor || source.stream().anyMatch(r -> r.contains(seq));
                boolean after = seq <= result.replayFloor() || result.ranges().stream().anyMatch(r -> r.contains(seq));
                assertThat(after).as("trial %s sequence %s", trial, seq).isEqualTo(before);
            }
            long previous = result.replayFloor();
            for (var range : result.ranges()) {
                assertThat(range.fromSequence()).isGreaterThan(previous + 1);
                previous = range.toSequence();
            }
            assertThat(normalize(result.replayFloor(), 50, result.ranges())).isEqualTo(result);
        }
    }
}
