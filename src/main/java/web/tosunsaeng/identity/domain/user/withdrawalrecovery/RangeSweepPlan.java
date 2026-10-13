package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.List;

/** Pure keyset-sweep plan. Cursor and carry key are advisory progress, never replay coverage. */
public final class RangeSweepPlan {
    private RangeSweepPlan() { }
    public record Plan(long replayFloor, List<UnavailableRanges.Range> replacements,
                       long nextAfterSequence, Long nextCarryFromSequence) {
        public Plan { replacements = List.copyOf(replacements); }
    }

    /** Fresh rows are strictly from>after, sorted/unique, limit99. Load carry by its exact key in
     * the same snapshot. CAS/row changes/cursor+carry update must commit together in the adapter. */
    public static Plan create(long floor, long head, long after, UnavailableRanges.Range carry,
                              List<UnavailableRanges.Range> fresh) {
        if (after < 0 || after > head || fresh.size() > 99
                || carry != null && carry.fromSequence() > after)
            throw new IllegalArgumentException("Invalid range sweep bounds.");
        long previous = after;
        for (var range : fresh) {
            if (range.fromSequence() <= previous) throw new IllegalArgumentException("Unordered range sweep.");
            previous = range.fromSequence();
        }
        var selected = new ArrayList<UnavailableRanges.Range>();
        if (carry != null) selected.add(carry);
        selected.addAll(fresh);
        var normalized = UnavailableRanges.normalize(floor, head, selected);
        if (fresh.isEmpty()) return new Plan(normalized.replayFloor(), normalized.ranges(), 0, null);
        Long nextCarry = normalized.ranges().isEmpty() ? null : normalized.ranges().getLast().fromSequence();
        return new Plan(normalized.replayFloor(), normalized.ranges(), previous, nextCarry);
    }
}
