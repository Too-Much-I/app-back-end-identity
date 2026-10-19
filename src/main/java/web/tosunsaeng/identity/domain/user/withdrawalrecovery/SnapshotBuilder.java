package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** One claimed build, one Mongo snapshot session. Never resumes partial rows at a different T. */
public final class SnapshotBuilder {
    public interface Source {
        MongoRecoveryReader.SnapshotResult read(String environment, String stream,
                Consumer<MongoRecoveryReader.SnapshotStart> start, Consumer<SnapshotDigest.Item> rows);
    }
    public enum Outcome { NOT_CLAIMED, READY, FAILED }
    private final MongoSnapshotStore store;
    private final Source source;
    public SnapshotBuilder(MongoSnapshotStore store, Source source) {
        this.store = Objects.requireNonNull(store); this.source = Objects.requireNonNull(source);
    }
    /** One bounded discovery/claim, not an unbounded scheduler loop. */
    public Outcome buildNext(String environment) {
        var pending = store.nextPending(environment);
        return pending == null ? Outcome.NOT_CLAIMED : build(pending.owner(), pending.snapshotId());
    }
    public Outcome build(RecoveryCursor.Owner owner, String snapshotId) {
        var lease = store.claim(owner, snapshotId);
        if (lease == null) return Outcome.NOT_CLAIMED;
        try {
            var batch = new ArrayList<SnapshotDigest.Item>(20);
            var read = source.read(owner.environment(), lease.streamId(), start -> store.pin(lease, start), item -> {
                batch.add(item);
                if (batch.size() == 20) { store.append(lease, List.copyOf(batch)); batch.clear(); }
            });
            if (!batch.isEmpty()) store.append(lease, List.copyOf(batch));
            store.beginVerification(lease);
            var verified = store.verifyStored(lease);
            store.seal(lease, read, verified);
            return Outcome.READY;
        } catch (RuntimeException failure) {
            String code = failure instanceof MongoRecoveryReader.Unavailable readFailure
                    ? readFailure.failureCode() : "SNAPSHOT_BUILD_FAILED";
            store.fail(lease, code);
            // Read the durable result: an uncertain seal commit may already have produced READY.
            return "READY".equals(store.status(owner, snapshotId).status()) ? Outcome.READY : Outcome.FAILED;
        }
    }
}
