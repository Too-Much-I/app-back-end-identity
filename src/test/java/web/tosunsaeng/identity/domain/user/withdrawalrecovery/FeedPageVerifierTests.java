package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.FeedPageVerifier.*;

class FeedPageVerifierTests {
    private static final String STREAM = "11111111-1111-4111-8111-111111111111";
    private static final String OTHER = "22222222-2222-4222-8222-222222222222";
    private static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");

    private Journal row(long sequence) { return row(sequence, NOW); }
    private Journal row(long sequence, Instant capturedAt) {
        String event = new java.util.UUID(0, sequence).toString();
        return new Journal(event, STREAM, sequence, event, OTHER, NOW, capturedAt);
    }
    private Observation source(long head, long floor, List<UnavailableRanges.Range> ranges, List<Journal> rows) {
        return new Observation(STREAM, head, floor, NOW, ranges, rows);
    }
    private void rejected(Runnable action, Failure failure, Long sequence, UnavailableRanges.Range range) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(Rejected.class, e -> {
            assertThat(e.failure()).isEqualTo(failure);
            assertThat(e.missingSequence()).isEqualTo(sequence);
            assertThat(e.unavailableRange()).isEqualTo(range);
            assertThat(e.getMessage()).isEqualTo(failure.name());
        });
    }

    @Test void readsBoundedPageAndKeepsH2WhenHeadAdvances() {
        var rows = LongStream.rangeClosed(101, 120).mapToObj(this::row).toList();
        var result = verify(STREAM, 100, 125, source(150, 80, List.of(), rows));
        assertThat(result.items()).hasSize(20);
        assertThat(result.nextAfterSequence()).isEqualTo(120);
        assertThat(result.targetThroughSequence()).isEqualTo(125);
        assertThat(result.done()).isFalse();
        assertThat(result.toString()).doesNotContain(OTHER);
    }

    @Test void holeRejectsWholePageRatherThanReturningItsValidPrefix() {
        rejected(() -> verify(STREAM, 0, 3, source(3, 0, List.of(), List.of(row(1), row(3)))),
                Failure.COVERAGE_GAP, 2L, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of())),
                Failure.COVERAGE_GAP, 1L, null);
    }

    @Test void reportsEarliestRequiredFailureNotALaterKnownRange() {
        rejected(() -> verify(STREAM, 0, 3, source(3, 0,
                        List.of(new UnavailableRanges.Range(3, 3)), List.of(row(2)))),
                Failure.COVERAGE_GAP, 1L, null);
    }

    @Test void prefixAndMiddleExpiryReturnExactEvidence() {
        rejected(() -> verify(STREAM, 3, 5, source(10, 8, List.of(), List.of())),
                Failure.REPLAY_UNAVAILABLE, 4L, new UnavailableRanges.Range(1, 8));
        var range = new UnavailableRanges.Range(5, 10);
        rejected(() -> verify(STREAM, 3, 7, source(10, 0, List.of(range), List.of(row(4)))),
                Failure.REPLAY_UNAVAILABLE, 5L, range);
    }

    @Test void logicalExpiryWorksAtExactBoundaryWithoutPhysicalCleanup() {
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of(row(1, NOW.minus(RETENTION))))),
                Failure.REPLAY_UNAVAILABLE, 1L, new UnavailableRanges.Range(1, 1));
        assertThat(verify(STREAM, 0, 1, source(1, 0, List.of(),
                List.of(row(1, NOW.minus(RETENTION).plusNanos(1))))).done()).isTrue();
    }

    @Test void completedRangeIncludingMaxValueIsTheOnlyEmptySuccess() {
        assertThat(verify(STREAM, Long.MAX_VALUE, Long.MAX_VALUE,
                source(Long.MAX_VALUE, Long.MAX_VALUE, List.of(), List.of())).done()).isTrue();
        assertThat(verify(STREAM, 0, 0, source(0, 0, List.of(), List.of())).items()).isEmpty();
        assertThat(verify(STREAM, Long.MAX_VALUE - 1, Long.MAX_VALUE,
                source(Long.MAX_VALUE, 0, List.of(), List.of(row(Long.MAX_VALUE)))).nextAfterSequence())
                .isEqualTo(Long.MAX_VALUE);
    }

    @Test void contradictionsHaveNoFalseGapDetails() {
        rejected(() -> verify(STREAM, 0, 2, source(1, 0, List.of(), List.of())),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 2, List.of(), List.of())),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of(row(1), row(1)))),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of(row(2)))),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of(row(1, NOW.plusSeconds(1))))),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(new UnavailableRanges.Range(1, 2)), List.of())),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
    }

    @Test void rejectsInvalidBoundsChangedStreamAndCrossStreamRows() {
        rejected(() -> verify(STREAM, -1, 1, source(1, 0, List.of(), List.of())), Failure.INVALID_BOUNDS, null, null);
        rejected(() -> verify(STREAM, 2, 1, source(2, 0, List.of(), List.of())), Failure.INVALID_BOUNDS, null, null);
        rejected(() -> verify(OTHER, 0, 1, source(1, 0, List.of(), List.of())), Failure.STREAM_REPLACED, null, null);
        var wrong = new Journal(OTHER, OTHER, 1, OTHER, OTHER, NOW, NOW);
        rejected(() -> verify(STREAM, 0, 1, source(1, 0, List.of(), List.of(wrong))),
                Failure.SOURCE_AVAILABILITY_UNCERTAIN, null, null);
    }
}
