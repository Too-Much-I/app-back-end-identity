package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

/** Pure precedence rules. Lookup, authenticated owner checks and atomic creation remain adapter duties. */
public final class SnapshotAccessRules {
    private SnapshotAccessRules() { }
    public enum Action { CREATE, STATUS, PAGE }
    public enum State { BUILDING, READY, FAILED, EXPIRED }
    public enum Outcome { CREATE_NEW, BUILDING, READY, FAILED, EXPIRED, PAGE,
        OPERATION_CONFLICT, STREAM_REPLACED, SNAPSHOT_NOT_FOUND,
        SNAPSHOT_NOT_READY, SNAPSHOT_FAILED, SNAPSHOT_EXPIRED }

    public static Outcome decide(Action action, boolean exists, boolean bindingMatches, boolean active,
                                 boolean expired, State state) {
        if (action == null || exists && state == null) throw new IllegalArgumentException("Invalid snapshot state.");
        if (!exists) {
            if (action != Action.CREATE) return Outcome.SNAPSHOT_NOT_FOUND;
            return active ? Outcome.CREATE_NEW : Outcome.STREAM_REPLACED;
        }
        if (action == Action.CREATE && !bindingMatches) return Outcome.OPERATION_CONFLICT;
        if (!active) return Outcome.STREAM_REPLACED;
        if (expired || state == State.EXPIRED)
            return action == Action.PAGE ? Outcome.SNAPSHOT_EXPIRED : Outcome.EXPIRED;
        if (action != Action.PAGE) return Outcome.valueOf(state.name());
        return switch (state) {
            case BUILDING -> Outcome.SNAPSHOT_NOT_READY;
            case FAILED -> Outcome.SNAPSHOT_FAILED;
            case READY -> Outcome.PAGE;
            case EXPIRED -> Outcome.SNAPSHOT_EXPIRED;
        };
    }
}
