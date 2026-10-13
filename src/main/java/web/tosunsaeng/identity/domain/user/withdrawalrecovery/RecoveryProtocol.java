package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/** Isolated recovery codec. Not registered as a controller or a global Jackson configuration. */
public final class RecoveryProtocol {
    public static final int MAX_BYTES = 16_384;
    public static final int MAX_CURSOR_CHARS = 2_048;
    public static final String PREFIX = "/internal/v1/billing/withdrawals";
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private RecoveryProtocol() { }

    public enum Route {
        CREATE("/snapshots"), STATUS("/snapshots/status"), SNAPSHOT_PAGE("/snapshots/page"), FEED_PAGE("/feed/page");
        private final String suffix;
        Route(String suffix) { this.suffix = suffix; }
        public String path() { return PREFIX + suffix; }
    }

    public sealed interface Request permits Create, Status, SnapshotPage, FeedPage { }
    public record Create(String streamId, String operationId) implements Request {
        @Override public String toString() { return "RecoveryCreate[redacted]"; }
    }
    public record Status(String snapshotId) implements Request {
        @Override public String toString() { return "RecoveryStatus[redacted]"; }
    }
    public record SnapshotPage(String snapshotId, String cursor) implements Request {
        @Override public String toString() { return "RecoverySnapshotPage[redacted]"; }
    }
    public record FeedPage(String streamId, long afterSequence, String scanCursor) implements Request {
        @Override public String toString() { return "RecoveryFeedPage[redacted]"; }
    }

    public enum Error {
        INVALID_REQUEST(400, false, "Invalid recovery request."),
        SNAPSHOT_NOT_FOUND(404, false, "Snapshot not found."),
        REQUEST_TOO_LARGE(413, false, "Recovery request exceeds the byte limit."),
        UNSUPPORTED_CONTRACT(422, false, "Unsupported recovery contract."),
        OPERATION_CONFLICT(409, false, "Operation is bound to a different request."),
        STREAM_REPLACED(409, false, "Withdrawal stream has been replaced."),
        SNAPSHOT_NOT_READY(409, true, "Snapshot is still building."),
        SNAPSHOT_FAILED(409, false, "Snapshot build failed. Check snapshot status."),
        COVERAGE_GAP(409, false, "Required withdrawal source is missing."),
        REPLAY_UNAVAILABLE(410, false, "Required withdrawal source is no longer replayable."),
        SNAPSHOT_EXPIRED(410, false, "Snapshot has expired."),
        CURSOR_EXPIRED(410, false, "Recovery cursor has expired."),
        SOURCE_AVAILABILITY_UNCERTAIN(503, true, "Withdrawal source availability is uncertain."),
        SOURCE_UNAVAILABLE(503, true, "Withdrawal source is temporarily unavailable."),
        RESPONSE_LIMIT_EXCEEDED(500, false, "Recovery response exceeds the byte limit.");

        private final int status;
        private final boolean retryable;
        private final String message;
        Error(int status, boolean retryable, String message) {
            this.status = status; this.retryable = retryable; this.message = message;
        }
        public int status() { return status; }
        public boolean retryable() { return retryable; }
        public String message() { return message; }
    }

    public static final class Invalid extends RuntimeException {
        private final Error error;
        private Invalid(Error error) { super(error.message()); this.error = error; }
        public Error error() { return error; }
    }

    /** Authentication precedes decoding in the future HTTP adapter. Header cardinality is its responsibility. */
    public static Request decode(Route route, byte[] utf8Body, String idempotencyKey) {
        if (utf8Body == null) throw invalid();
        if (utf8Body.length > MAX_BYTES) throw new Invalid(Error.REQUEST_TOO_LARGE);
        try {
            String body = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(utf8Body)).toString();
            JsonNode root = JSON.readTree(body);
            return switch (route) {
                case CREATE -> {
                    fields(root, Set.of("streamId"));
                    yield new Create(uuid4(text(root.get("streamId"))), uuid4(idempotencyKey));
                }
                case STATUS -> {
                    fields(root, Set.of("snapshotId"));
                    yield new Status(uuid4(text(root.get("snapshotId"))));
                }
                case SNAPSHOT_PAGE -> {
                    fields(root, Set.of("snapshotId", "cursor"));
                    yield new SnapshotPage(uuid4(text(root.get("snapshotId"))), cursor(root.get("cursor")));
                }
                case FEED_PAGE -> {
                    fields(root, Set.of("streamId", "afterSequence", "scanCursor"));
                    yield new FeedPage(uuid4(text(root.get("streamId"))), decimal(root.get("afterSequence")),
                            cursor(root.get("scanCursor")));
                }
            };
        } catch (Invalid known) { throw known; }
        catch (CharacterCodingException | com.fasterxml.jackson.core.JsonProcessingException failure) { throw invalid(); }
    }

    public static long decimal(JsonNode value) {
        String s = text(value);
        if (!s.matches("0|[1-9][0-9]{0,18}")) throw invalid();
        try { return Long.parseLong(s); }
        catch (NumberFormatException failure) { throw invalid(); }
    }

    public static String uuid4(String value) {
        try {
            UUID uuid = UUID.fromString(value);
            if (!uuid.toString().equals(value) || uuid.version() != 4 || uuid.variant() != 2) throw invalid();
            return value;
        } catch (IllegalArgumentException | NullPointerException failure) { throw invalid(); }
    }

    private static String text(JsonNode value) {
        if (value == null || !value.isTextual()) throw invalid();
        return value.textValue();
    }

    private static String cursor(JsonNode value) {
        if (value != null && value.isNull()) return null;
        String cursor = text(value);
        if (cursor.isEmpty() || cursor.length() > MAX_CURSOR_CHARS
                || !cursor.chars().allMatch(c -> c >= 0x21 && c <= 0x7e)) throw invalid();
        return cursor; // Format, signature, owner/stream binding and expiry belong to the signed-cursor adapter.
    }

    private static void fields(JsonNode root, Set<String> expected) {
        if (root == null || !root.isObject() || root.size() != expected.size()) throw invalid();
        for (String key : expected) if (!root.has(key)) throw invalid();
    }

    public static ObjectNode errorBody(Error error, String correlationId) {
        if (error == Error.COVERAGE_GAP || error == Error.REPLAY_UNAVAILABLE)
            throw new IllegalArgumentException("Source error requires verified details.");
        return base(error, correlationId);
    }

    public record SourceDetails(String streamId, long replayFloor, long observedThroughSequence,
                                long missingSequence, UnavailableRanges.Range unavailableRange,
                                java.time.Instant observedAt) { }

    public static ObjectNode sourceErrorBody(Error error, String correlationId, SourceDetails d) {
        if ((error != Error.COVERAGE_GAP && error != Error.REPLAY_UNAVAILABLE) || d == null
                || d.replayFloor() < 0 || d.replayFloor() > d.observedThroughSequence()
                || d.missingSequence() < 1 || d.missingSequence() > d.observedThroughSequence()
                || d.observedAt() == null)
            throw new IllegalArgumentException("Invalid source details.");
        uuid4(d.streamId());
        var range = d.unavailableRange();
        if (error == Error.REPLAY_UNAVAILABLE
                ? range == null || !range.contains(d.missingSequence()) || range.toSequence() > d.observedThroughSequence()
                : range != null || d.missingSequence() <= d.replayFloor())
            throw new IllegalArgumentException("Invalid source details.");
        var body = base(error, correlationId);
        var details = body.putObject("details");
        details.put("streamId", d.streamId()).put("replayFloor", Long.toString(d.replayFloor()))
                .put("observedThroughSequence", Long.toString(d.observedThroughSequence()))
                .put("missingSequence", Long.toString(d.missingSequence()))
                .put("observedAt", d.observedAt().toString());
        if (range == null) details.putNull("unavailableRange");
        else details.putObject("unavailableRange").put("fromSequence", Long.toString(range.fromSequence()))
                .put("toSequence", Long.toString(range.toSequence()));
        return body;
    }

    /** Serialize fully before writing HTTP bytes. Never return a truncated or empty success body. */
    public static byte[] encode(JsonNode response) {
        try {
            byte[] encoded = JSON.writeValueAsBytes(response);
            if (encoded.length > MAX_BYTES) throw new Invalid(Error.RESPONSE_LIMIT_EXCEEDED);
            return encoded;
        } catch (com.fasterxml.jackson.core.JsonProcessingException failure) {
            throw new Invalid(Error.RESPONSE_LIMIT_EXCEEDED);
        }
    }

    private static ObjectNode base(Error error, String correlationId) {
        return JSON.createObjectNode().put("code", error.name()).put("message", error.message())
                .put("retryable", error.retryable()).put("correlationId", uuid4(correlationId));
    }
    private static Invalid invalid() { return new Invalid(Error.INVALID_REQUEST); }
}
