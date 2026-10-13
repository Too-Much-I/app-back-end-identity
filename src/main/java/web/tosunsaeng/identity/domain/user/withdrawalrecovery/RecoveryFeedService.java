package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;

/** Application boundary. Owner must come from verified service authentication, never request fields. */
public final class RecoveryFeedService {
    public interface Source {
        MongoRecoveryReader.FeedRead read(String environment, String streamId, long after, Long target);
    }
    public record Reply(int status, byte[] body) {
        public Reply { body = body.clone(); }
        @Override public byte[] body() { return body.clone(); }
        @Override public String toString() { return "RecoveryReply[status=" + status + "]"; }
    }
    private final Source source;
    private final RecoveryCursor cursors;
    public RecoveryFeedService(Source source, RecoveryCursor cursors) {
        this.source = Objects.requireNonNull(source); this.cursors = Objects.requireNonNull(cursors);
    }
    public Reply page(RecoveryCursor.Owner authenticatedOwner, RecoveryProtocol.FeedPage request, String correlationId) {
        Objects.requireNonNull(authenticatedOwner); Objects.requireNonNull(request);
        RecoveryProtocol.uuid4(correlationId);
        try {
            Long target = null;
            if (request.scanCursor() != null) {
                var verified = cursors.verify(request.scanCursor(), authenticatedOwner, RecoveryCursor.Kind.FEED, request.streamId(), null);
                RecoveryCursor.requireFeedPosition(verified, request.afterSequence());
                target = verified.binding().target();
            }
            var read = source.read(authenticatedOwner.environment(), request.streamId(), request.afterSequence(), target);
            var observation = read.observation();
            if (target != null && target.longValue() != read.target())
                return error(RecoveryProtocol.Error.SOURCE_AVAILABILITY_UNCERTAIN, correlationId);
            final FeedPageVerifier.Verified verified;
            try {
                verified = FeedPageVerifier.verify(request.streamId(), request.afterSequence(), read.target(), observation);
            } catch (FeedPageVerifier.Rejected failure) {
                var error = switch (failure.failure()) {
                    case INVALID_BOUNDS -> RecoveryProtocol.Error.INVALID_REQUEST;
                    case STREAM_REPLACED -> RecoveryProtocol.Error.STREAM_REPLACED;
                    case SOURCE_AVAILABILITY_UNCERTAIN -> RecoveryProtocol.Error.SOURCE_AVAILABILITY_UNCERTAIN;
                    case COVERAGE_GAP -> RecoveryProtocol.Error.COVERAGE_GAP;
                    case REPLAY_UNAVAILABLE -> RecoveryProtocol.Error.REPLAY_UNAVAILABLE;
                };
                if (failure.missingSequence() == null) return error(error, correlationId);
                var details = new RecoveryProtocol.SourceDetails(observation.streamId(), observation.replayFloor(),
                        observation.head(), failure.missingSequence(), failure.unavailableRange(), observation.observedAt());
                return new Reply(error.status(), RecoveryProtocol.encode(RecoveryProtocol.sourceErrorBody(error, correlationId, details)));
            }
            // Preserve original issue time and H2 on every continuation, including the completed page.
            String cursor = request.scanCursor() == null ? cursors.issue(new RecoveryCursor.Binding(authenticatedOwner,
                    RecoveryCursor.Kind.FEED, request.streamId(), null, request.afterSequence(), verified.targetThroughSequence())) : request.scanCursor();
            ObjectNode body = JsonNodeFactory.instance.objectNode();
            var items = body.putArray("items");
            for (var row : verified.items()) {
                var item = items.addObject().put("sequence", Long.toString(row.sequence()));
                item.putObject("event").put("eventId", row.eventId()).put("schemaVersion", 1)
                        .put("userId", row.userId()).put("withdrawnAt", row.withdrawnAt().toString());
            }
            body.put("scanCursor", cursor).put("targetThroughSequence", Long.toString(verified.targetThroughSequence()))
                    .put("nextAfterSequence", Long.toString(verified.nextAfterSequence()))
                    .put("scannedThrough", Long.toString(verified.nextAfterSequence())).put("done", verified.done());
            body.putObject("availability").put("streamId", observation.streamId())
                    .put("replayFloor", Long.toString(observation.replayFloor()))
                    .put("observedThroughSequence", Long.toString(observation.head()))
                    .put("checkedFromExclusive", Long.toString(request.afterSequence()))
                    .put("checkedThroughSequence", Long.toString(verified.nextAfterSequence()))
                    .put("observedAt", observation.observedAt().toString());
            return new Reply(200, RecoveryProtocol.encode(body));
        } catch (RecoveryCursor.Rejected failure) { return error(failure.error(), correlationId); }
        catch (RecoveryProtocol.Invalid failure) { return error(failure.error(), correlationId); }
        catch (MongoRecoveryReader.Unavailable failure) { return error(RecoveryProtocol.Error.SOURCE_AVAILABILITY_UNCERTAIN, correlationId); }
    }
    private Reply error(RecoveryProtocol.Error error, String correlationId) {
        return new Reply(error.status(), RecoveryProtocol.encode(RecoveryProtocol.errorBody(error, correlationId)));
    }
}
