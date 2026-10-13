package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords;

/** Point containment witnesses, not a truncated list of the stream's first N ranges. */
public final class PageRangeWitnesses {
    private PageRangeWitnesses() { }

    public interface SnapshotLookup {
        /** Same source snapshot as head/floor/journal. Query ALL ranges for this point before limit(1).
         * Filter streamId==stream, from<=sequence, to>=sequence; sort from ASC, to ASC, _id ASC.
         * Empty means verified absence, never query timeout/truncation/permission failure. */
        Optional<UnavailableRanges.Range> containing(String stream, long sequence);
    }

    public static List<UnavailableRanges.Range> read(String stream, long after, long target, SnapshotLookup lookup) {
        WithdrawalRecords.uuid(stream);
        if (after < 0 || target < after) throw new IllegalArgumentException("Invalid page bounds.");
        long end = after + Math.min(FeedPageVerifier.PAGE_SIZE, target - after);
        var witnesses = new ArrayList<UnavailableRanges.Range>();
        for (long sequence = after; sequence < end;) {
            sequence++;
            var witness = lookup.containing(stream, sequence);
            if (witness.isPresent()) {
                var range = witness.get();
                if (!range.contains(sequence)) throw new IllegalStateException("Invalid point containment proof.");
                if (!witnesses.contains(range)) witnesses.add(range);
            }
        }
        return List.copyOf(witnesses);
    }
}
