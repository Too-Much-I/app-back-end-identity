package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Bounded write plan, not a database transaction. Adapter must CAS stream + original rows atomically. */
public final class RangeMutationPlan {
    private RangeMutationPlan() { }
    public record Stored(String id, UnavailableRanges.Range range) {
        public Stored { Objects.requireNonNull(id); Objects.requireNonNull(range); }
    }
    public record Plan(long replayFloor, List<Stored> selected, List<UnavailableRanges.Range> replacements) {
        public Plan { selected = List.copyOf(selected); replacements = List.copyOf(replacements); }
    }

    /** exactAtSeedStart is the same-snapshot unique-key lookup, before selecting general candidates.
     * Never omit that lookup or turn its failure into null. An overflow candidate is left untouched. */
    public static Plan create(long floor, long head, long expiredSequence, List<Stored> candidates,
                              Stored exactAtSeedStart) {
        var seed = new UnavailableRanges.Range(expiredSequence, expiredSequence);
        if (candidates.size() > UnavailableRanges.MAX_BATCH - 1)
            throw new IllegalArgumentException("Too many range candidates.");
        if (exactAtSeedStart != null && exactAtSeedStart.range().fromSequence() != expiredSequence)
            throw new IllegalArgumentException("Incorrect exact-key lookup.");
        var selected = new ArrayList<Stored>();
        if (exactAtSeedStart != null) selected.add(exactAtSeedStart);
        var ids = new HashSet<String>();
        var starts = new HashSet<Long>();
        for (Stored row : candidates) {
            if (!ids.add(row.id()) || !starts.add(row.range().fromSequence()))
                throw new IllegalArgumentException("Contradictory stored ranges.");
            if (row.range().fromSequence() == expiredSequence && !row.equals(exactAtSeedStart))
                throw new IllegalArgumentException("Exact-key lookup disagrees with candidates.");
            if (exactAtSeedStart != null && row.id().equals(exactAtSeedStart.id()) && !row.equals(exactAtSeedStart))
                throw new IllegalArgumentException("Stored identity changed.");
        }
        for (Stored row : candidates.stream().sorted(Comparator.comparingLong(r -> r.range().fromSequence())).toList()) {
            if (row.equals(exactAtSeedStart)) continue;
            if (selected.size() < UnavailableRanges.MAX_BATCH - 1) selected.add(row);
        }
        var ranges = new ArrayList<>(selected.stream().map(Stored::range).toList());
        ranges.add(seed);
        var normalized = UnavailableRanges.normalize(floor, head, ranges);
        return new Plan(normalized.replayFloor(), selected, normalized.ranges());
    }
}
