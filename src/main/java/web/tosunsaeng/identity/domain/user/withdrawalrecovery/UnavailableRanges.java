package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounded, lossless normalization. Unselected database ranges must never be deleted. */
public final class UnavailableRanges {
    public static final int MAX_BATCH = 100;

    private UnavailableRanges() { }

    public record Range(long fromSequence, long toSequence) {
        public Range {
            if (fromSequence < 1 || toSequence < fromSequence)
                throw new IllegalArgumentException("Invalid source range.");
        }
        public boolean contains(long sequence) {
            return fromSequence <= sequence && sequence <= toSequence;
        }
    }

    public record Normalized(long replayFloor, List<Range> ranges) {
        public Normalized { ranges = List.copyOf(ranges); }
    }

    /** Accepts a selected batch, not the entire stream history. Does not infer missing ranges. */
    public static Normalized normalize(long replayFloor, long observedHead, List<Range> selected) {
        if (replayFloor < 0 || observedHead < replayFloor || selected.size() > MAX_BATCH)
            throw new IllegalArgumentException("Invalid normalization bounds.");
        var sorted = selected.stream().sorted(Comparator.comparingLong(Range::fromSequence)).toList();
        var merged = new ArrayList<Range>();
        long floor = replayFloor;
        for (Range range : sorted) {
            if (range.toSequence() > observedHead)
                throw new IllegalArgumentException("Source range exceeds head.");
            if (range.fromSequence() <= floor || adjacent(floor, range.fromSequence())) {
                floor = Math.max(floor, range.toSequence());
            } else if (!merged.isEmpty()) {
                Range previous = merged.getLast();
                if (range.fromSequence() <= previous.toSequence()
                        || adjacent(previous.toSequence(), range.fromSequence())) {
                    merged.set(merged.size() - 1,
                            new Range(previous.fromSequence(), Math.max(previous.toSequence(), range.toSequence())));
                } else merged.add(range);
            } else merged.add(range);
        }
        return new Normalized(floor, merged);
    }

    private static boolean adjacent(long left, long right) {
        return left != Long.MAX_VALUE && left + 1 == right;
    }
}
