package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.Journal;

/** Internal page validation, NOT a repository or HTTP adapter. No consumer checkpoint authority. */
public final class FeedPageVerifier {
    public static final int PAGE_SIZE = 20;
    private FeedPageVerifier() { }

    public enum Failure { INVALID_BOUNDS, STREAM_REPLACED, SOURCE_AVAILABILITY_UNCERTAIN,
        COVERAGE_GAP, REPLAY_UNAVAILABLE }

    public static final class Rejected extends RuntimeException {
        private final Failure failure;
        private final Long missingSequence;
        private final UnavailableRanges.Range unavailableRange;
        private Rejected(Failure failure, Long sequence, UnavailableRanges.Range range) {
            super(failure.name());
            this.failure = failure; this.missingSequence = sequence; this.unavailableRange = range;
        }
        public Failure failure() { return failure; }
        public Long missingSequence() { return missingSequence; }
        public UnavailableRanges.Range unavailableRange() { return unavailableRange; }
    }

    /** Must come from ONE trusted, short snapshot read including stream, counter, ranges and rows.
     * Ranges must witness every unavailable point in the planned page (see PageRangeWitnesses).
     * They need not include every overlapping interval. Do not truncate a global interval scan.
     * An adapter must not assemble this from independent majority/live reads. */
    public record Observation(String streamId, long head, long replayFloor, Instant observedAt,
                              List<UnavailableRanges.Range> ranges, List<Journal> rows) {
        public Observation {
            WithdrawalRecords.uuid(streamId);
            Objects.requireNonNull(observedAt);
            ranges = List.copyOf(ranges); rows = List.copyOf(rows);
        }
        @Override public String toString() { return "FeedObservation[redacted]"; }
    }

    public record Verified(List<Journal> items, long nextAfterSequence, long targetThroughSequence,
                           boolean done) {
        public Verified { items = List.copyOf(items); }
        @Override public String toString() { return "VerifiedFeedPage[redacted]"; }
    }

    /** target is a previously authenticated cursor's H2, or this observation's head on first read. */
    public static Verified verify(String requestedStream, long after, long target, Observation source) {
        WithdrawalRecords.uuid(requestedStream);
        if (!requestedStream.equals(source.streamId())) throw reject(Failure.STREAM_REPLACED);
        if (after < 0 || target < after) throw reject(Failure.INVALID_BOUNDS);
        if (source.head() < target || source.replayFloor() < 0 || source.replayFloor() > source.head())
            throw reject(Failure.SOURCE_AVAILABILITY_UNCERTAIN);
        long end = after + Math.min(PAGE_SIZE, target - after);
        if (source.rows().size() > PAGE_SIZE || source.ranges().size() > PAGE_SIZE)
            throw reject(Failure.SOURCE_AVAILABILITY_UNCERTAIN);
        for (var range : source.ranges()) {
            if (range.toSequence() > source.head()) throw reject(Failure.SOURCE_AVAILABILITY_UNCERTAIN);
        }
        var rows = new HashMap<Long, Journal>();
        for (Journal row : source.rows()) {
            if (!source.streamId().equals(row.streamId()) || row.sequence() <= after || row.sequence() > end
                    || row.capturedAt().isAfter(source.observedAt())
                    || rows.put(row.sequence(), row) != null)
                throw reject(Failure.SOURCE_AVAILABILITY_UNCERTAIN);
        }
        var items = new java.util.ArrayList<Journal>();
        // Do not compute after+1 for an empty range; MAX_VALUE is a valid completed target.
        for (long sequence = after; sequence < end;) {
            sequence++;
            if (sequence <= source.replayFloor()) throw unavailable(sequence,
                    new UnavailableRanges.Range(1, source.replayFloor()));
            for (var range : source.ranges()) if (range.contains(sequence)) throw unavailable(sequence, range);
            Journal row = rows.get(sequence);
            if (row == null) throw new Rejected(Failure.COVERAGE_GAP, sequence, null);
            if (!source.observedAt().isBefore(row.expiresAt()))
                throw unavailable(sequence, new UnavailableRanges.Range(sequence, sequence));
            items.add(row);
        }
        return new Verified(items, end, target, end == target);
    }

    private static Rejected reject(Failure failure) { return new Rejected(failure, null, null); }
    private static Rejected unavailable(long sequence, UnavailableRanges.Range range) {
        return new Rejected(Failure.REPLAY_UNAVAILABLE, sequence, range);
    }
}
