package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.Objects;
import java.util.function.Supplier;

/** Snapshot application API; trusted owner comes from future workload ingress, not JSON or user JWT. */
public final class RecoverySnapshotService {
    public record Reply(int status, byte[] body, Integer retryAfterSeconds) {
        public Reply { body = body.clone(); }
        @Override public byte[] body() { return body.clone(); }
        @Override public String toString() { return "SnapshotReply[status=" + status + "]"; }
    }
    private final MongoSnapshotStore store;
    private final RecoveryCursor cursors;
    public RecoverySnapshotService(MongoSnapshotStore store, RecoveryCursor cursors) {
        this.store = Objects.requireNonNull(store); this.cursors = Objects.requireNonNull(cursors);
    }
    public Reply create(RecoveryCursor.Owner owner, RecoveryProtocol.Create request, String correlationId) {
        return guarded(correlationId, () -> statusReply(store.create(owner, request), true));
    }
    public Reply status(RecoveryCursor.Owner owner, RecoveryProtocol.Status request, String correlationId) {
        return guarded(correlationId, () -> statusReply(store.status(owner, request.snapshotId()), false));
    }
    public Reply page(RecoveryCursor.Owner owner, RecoveryProtocol.SnapshotPage request, String correlationId) {
        return guarded(correlationId, () -> {
            var state = store.status(owner, request.snapshotId());
            // Preserve state/expiry precedence, including for malformed/expired page cursors.
            if (!"READY".equals(state.status())) throw new MongoSnapshotStore.Rejected(switch (state.status()) {
                case "BUILDING" -> RecoveryProtocol.Error.SNAPSHOT_NOT_READY;
                case "FAILED" -> RecoveryProtocol.Error.SNAPSHOT_FAILED;
                case "EXPIRED" -> RecoveryProtocol.Error.SNAPSHOT_EXPIRED;
                default -> RecoveryProtocol.Error.SOURCE_UNAVAILABLE;
            });
            RecoveryCursor.Verified verified = request.cursor() == null ? null : cursors.verify(request.cursor(), owner,
                    RecoveryCursor.Kind.SNAPSHOT, state.streamId(), request.snapshotId());
            long ordinal = verified == null ? 0 : verified.binding().start();
            var page = store.page(owner, request.snapshotId(), state.streamId(), ordinal,
                    verified == null ? -1 : verified.binding().target(), verified == null ? null : verified.binding().afterUser());
            var body = JsonNodeFactory.instance.objectNode();
            var items = body.putArray("items");
            for (var item : page.items()) items.addObject().put("userId", item.userId()).put("withdrawnAt", item.withdrawnAt().toString());
            if (page.done()) body.putNull("nextCursor");
            else {
                var next = new RecoveryCursor.Binding(owner, RecoveryCursor.Kind.SNAPSHOT, state.streamId(), request.snapshotId(),
                        Math.incrementExact(ordinal), page.pageCount(), page.lastUser());
                body.put("nextCursor", verified == null ? cursors.issue(next) : cursors.advance(verified, next));
            }
            body.put("done", page.done());
            return new Reply(200, RecoveryProtocol.encode(body), null);
        });
    }
    private Reply statusReply(MongoSnapshotStore.View view, boolean create) {
        var body = JsonNodeFactory.instance.objectNode().put("snapshotId", view.snapshotId()).put("status", view.status());
        if (view.manifest() != null) body.set("manifest", view.manifest());
        if (view.failureCode() != null) body.put("failureCode", view.failureCode());
        boolean building = "BUILDING".equals(view.status());
        return new Reply(create && building ? 202 : 200, RecoveryProtocol.encode(body), building ? 5 : null);
    }
    private Reply guarded(String id, Supplier<Reply> work) {
        RecoveryProtocol.uuid4(id);
        try { return work.get(); }
        catch (MongoSnapshotStore.Rejected failure) { return error(failure.error(), id); }
        catch (RecoveryProtocol.Invalid failure) { return error(failure.error(), id); }
        catch (RecoveryCursor.Rejected failure) { return error(failure.error(), id); }
        catch (MongoRecoveryReader.Unavailable | org.springframework.dao.DataAccessException failure) {
            return error(RecoveryProtocol.Error.SOURCE_UNAVAILABLE, id);
        }
    }
    private Reply error(RecoveryProtocol.Error code, String id) {
        return new Reply(code.status(), RecoveryProtocol.encode(RecoveryProtocol.errorBody(code, id)), null);
    }
}
