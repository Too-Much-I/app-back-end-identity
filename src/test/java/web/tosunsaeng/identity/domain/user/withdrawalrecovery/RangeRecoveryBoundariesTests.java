package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.UnavailableRanges.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocolTests.ID;

class RangeRecoveryBoundariesTests {
    @Test void sharedRangeRecipesMatchPointLookupAndNormalization() throws Exception {
        var fixture = RecoveryProtocolTests.fixture("range-cases.json");
        for (var test : fixture.get("rangeLookupCases")) {
            var g = test.get("generator");
            long start = Long.parseLong(g.get("fromStart").asText());
            long step = Long.parseLong(g.get("fromStep").asText());
            var rows = LongStream.range(0, g.get("count").asInt()).mapToObj(n -> {
                long from = start + n * step;
                long to = g.has("toFixed") ? Long.parseLong(g.get("toFixed").asText())
                        : from + Long.parseLong(g.get("width").asText());
                return new Range(from, to);
            }).toList();
            var calls = new AtomicInteger();
            var actual = PageRangeWitnesses.read(ID, Long.parseLong(test.get("after").asText()),
                    Long.parseLong(test.get("target").asText()), (s, n) -> {
                        calls.incrementAndGet(); return rows.stream().filter(r -> r.contains(n)).findFirst();
                    });
            var expected = new ArrayList<Range>();
            for (var r : test.get("expectedWitnesses"))
                expected.add(new Range(Long.parseLong(r.get(0).asText()), Long.parseLong(r.get(1).asText())));
            assertThat(actual).as(test.get("name").asText()).isEqualTo(expected);
            assertThat(calls.get()).isEqualTo(test.get("expectedLookupCount").asInt());
        }
        for (var test : fixture.get("normalizationCases")) {
            var ranges = new ArrayList<Range>(); var expected = new ArrayList<Range>();
            for (var r : test.get("ranges")) ranges.add(new Range(Long.parseLong(r.get(0).asText()), Long.parseLong(r.get(1).asText())));
            for (var r : test.get("expectedRanges")) expected.add(new Range(Long.parseLong(r.get(0).asText()), Long.parseLong(r.get(1).asText())));
            assertThat(normalize(Long.parseLong(test.get("floor").asText()), Long.parseLong(test.get("head").asText()), ranges))
                    .isEqualTo(new Normalized(Long.parseLong(test.get("expectedFloor").asText()), expected));
        }
    }

    @Test void pointLookupFindsThe101stRangeWithoutGlobalTruncation() {
        var all = LongStream.rangeClosed(1, 150).mapToObj(n -> new Range(2 * n, 2 * n)).toList();
        var calls = new AtomicInteger();
        var witnesses = PageRangeWitnesses.read(ID, 201, 202, (stream, sequence) -> {
            calls.incrementAndGet();
            return all.stream().filter(r -> r.contains(sequence)).findFirst();
        });
        assertThat(witnesses).containsExactly(new Range(202, 202));
        assertThat(calls.get()).isEqualTo(1);
        assertThatThrownBy(() -> FeedPageVerifier.verify(ID, 201, 202,
                new FeedPageVerifier.Observation(ID, 300, 0, Instant.EPOCH, witnesses, List.of())))
                .isInstanceOfSatisfying(FeedPageVerifier.Rejected.class,
                        e -> assertThat(e.failure()).isEqualTo(FeedPageVerifier.Failure.REPLAY_UNAVAILABLE));
    }

    @Test void moreThan100OverlapsNeedOnlyOneWitnessPerPoint() {
        var all = LongStream.rangeClosed(1, 150).mapToObj(n -> new Range(n, 200)).toList();
        var calls = new AtomicInteger();
        var witnesses = PageRangeWitnesses.read(ID, 150, 180, (stream, sequence) -> {
            calls.incrementAndGet();
            return all.stream().filter(r -> r.contains(sequence)).findFirst();
        });
        assertThat(calls.get()).isEqualTo(20);
        assertThat(witnesses).containsExactly(new Range(1, 200));
    }

    @Test void noLookupForCompletedMaxAndFailuresNeverBecomeAbsence() {
        assertThat(PageRangeWitnesses.read(ID, Long.MAX_VALUE, Long.MAX_VALUE,
                (s, n) -> { throw new AssertionError("Empty range must not query."); })).isEmpty();
        assertThatThrownBy(() -> PageRangeWitnesses.read(ID, 0, 1,
                (s, n) -> { throw new IllegalStateException("synthetic unavailable"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PageRangeWitnesses.read(ID, 0, 1,
                (s, n) -> Optional.of(new Range(2, 2)))).isInstanceOf(IllegalStateException.class);
    }

    @Test void exactKeyOutsideBatchIsIncludedByDisplacingOneCandidate() {
        var selected = LongStream.rangeClosed(1, 99)
                .mapToObj(n -> new RangeMutationPlan.Stored("r" + n, new Range(2 * n, 2 * n))).toList();
        var outside = new RangeMutationPlan.Stored("outside", new Range(201, 220));
        var plan = RangeMutationPlan.create(0, 220, 201, selected, outside);
        assertThat(plan.selected()).hasSize(99).contains(outside).doesNotContain(selected.getLast());
        assertThat(plan.replacements()).contains(new Range(201, 220));
        assertThat(plan.selected().stream().map(RangeMutationPlan.Stored::id).distinct().count()).isEqualTo(99);
        // Only selected originals can be mutated. The displaced row remains available to the next batch.
        var untouched = selected.stream().filter(r -> !plan.selected().contains(r)).toList();
        assertThat(untouched).containsExactly(selected.getLast());
    }

    @Test void sequentialBatchesConsume101AdjacentRangesWithoutLosingProgress() {
        var stored = new ArrayList<>(LongStream.rangeClosed(2, 102)
                .mapToObj(n -> new RangeMutationPlan.Stored("r" + n, new Range(n, n))).toList());
        long floor = 0;
        for (int i = 0; i < 2; i++) {
            var candidates = stored.stream().sorted(Comparator.comparingLong(r -> r.range().fromSequence())).limit(99).toList();
            var plan = RangeMutationPlan.create(floor, 102, 1, candidates, null);
            floor = plan.replayFloor();
            stored.removeAll(plan.selected());
            for (var range : plan.replacements()) stored.add(new RangeMutationPlan.Stored("merged" + range.fromSequence(), range));
        }
        assertThat(floor).isEqualTo(102);
        assertThat(stored).isEmpty();
    }

    @Test void inconsistentSameKeyLookupIsRejectedInsteadOfInfiniteIdenticalRetry() {
        var row = new RangeMutationPlan.Stored("same", new Range(5, 8));
        assertThatThrownBy(() -> RangeMutationPlan.create(0, 10, 5, List.of(row), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RangeMutationPlan.create(0, 10, 5, List.of(row),
                new RangeMutationPlan.Stored("other", new Range(5, 9))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void planDoesNotOverflowOrBridgeAvailableSequence() {
        var plan = RangeMutationPlan.create(100, Long.MAX_VALUE, Long.MAX_VALUE,
                List.of(new RangeMutationPlan.Stored("a", new Range(105, 108))), null);
        assertThat(plan.replayFloor()).isEqualTo(100);
        assertThat(plan.replacements()).containsExactly(new Range(105, 108), new Range(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test void sweepCarriesBoundaryRangeAndAdvancesPastUnchangedFirst99() {
        var first = LongStream.rangeClosed(1, 99).mapToObj(n -> new Range(n * 2, n * 2)).toList();
        var p1 = RangeSweepPlan.create(0, 300, 0, null, first);
        assertThat(p1.nextAfterSequence()).isEqualTo(198);
        assertThat(p1.nextCarryFromSequence()).isEqualTo(198);
        // The adjacent pair crosses the batch boundary. The last prior range must participate.
        var p2 = RangeSweepPlan.create(0, 300, p1.nextAfterSequence(), new Range(198, 198),
                List.of(new Range(199, 200), new Range(202, 202)));
        assertThat(p2.replacements()).containsExactly(new Range(198, 200), new Range(202, 202));
        assertThat(p2.nextAfterSequence()).isEqualTo(202);
        var p3 = RangeSweepPlan.create(0, 300, 202, new Range(202, 202), List.of());
        assertThat(p3.nextAfterSequence()).isZero();
        assertThat(p3.nextCarryFromSequence()).isNull();
        // Reset permits evidence inserted behind the cursor to be visited in the next sweep.
        assertThat(RangeSweepPlan.create(0, 300, p3.nextAfterSequence(), null, List.of(new Range(1, 2))).replayFloor())
                .isEqualTo(2);
    }

    @Test void sweepMaxAndInvalidCursorAreBounded() {
        var max = RangeSweepPlan.create(0, Long.MAX_VALUE, Long.MAX_VALUE - 1, null,
                List.of(new Range(Long.MAX_VALUE, Long.MAX_VALUE)));
        assertThat(max.nextAfterSequence()).isEqualTo(Long.MAX_VALUE);
        assertThat(RangeSweepPlan.create(0, Long.MAX_VALUE, max.nextAfterSequence(),
                max.replacements().getLast(), List.of()).nextAfterSequence()).isZero();
        assertThatThrownBy(() -> RangeSweepPlan.create(0, 10, 5, null, List.of(new Range(5, 6))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
