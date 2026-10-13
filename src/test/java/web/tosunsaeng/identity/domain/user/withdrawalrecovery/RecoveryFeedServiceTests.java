package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import web.tosunsaeng.identity.domain.user.withdrawalcapture.WithdrawalRecords.Journal;

class RecoveryFeedServiceTests {
    static final String STREAM = "00000000-0000-4000-8000-000000000001";
    static final String OTHER = "00000000-0000-4000-8000-000000000002";
    static final Instant NOW = Instant.parse("2026-10-12T00:00:00Z");
    final RecoveryCursor.Owner owner = new RecoveryCursor.Owner("test", "billing");
    final RecoveryFeedService.Source source = mock(RecoveryFeedService.Source.class);
    final RecoveryCursor cursors = new RecoveryCursor(Map.of("test", new byte[32]), "test", Clock.fixed(NOW, ZoneOffset.UTC));
    final RecoveryFeedService service = new RecoveryFeedService(source, cursors);
    JsonNode body(RecoveryFeedService.Reply reply) throws Exception { return new ObjectMapper().readTree(reply.body()); }
    MongoRecoveryReader.FeedRead observation(long target, long head, long floor, List<Journal> rows) {
        return new MongoRecoveryReader.FeedRead(new FeedPageVerifier.Observation(STREAM, head, floor, NOW, List.of(), rows), target);
    }
    Journal fact(long n) { return new Journal(OTHER, STREAM, n, OTHER, OTHER, NOW.minusSeconds(1), NOW.minusSeconds(1)); }
    @Test void successAndReplayPreserveCursorTargetAndIssueTime() throws Exception {
        when(source.read("test", STREAM, 0, null)).thenReturn(observation(1, 1, 0, List.of(fact(1))));
        var first = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, null), OTHER);
        assertThat(first.status()).isEqualTo(200);
        JsonNode firstBody = body(first); String cursor = firstBody.get("scanCursor").asText();
        assertThat(firstBody.get("done").asBoolean()).isTrue();
        assertThat(firstBody.get("items").get(0).get("event").get("schemaVersion").isInt()).isTrue();
        when(source.read("test", STREAM, 1, 1L)).thenReturn(observation(1, 2, 0, List.of()));
        var next = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 1, cursor), OTHER);
        assertThat(body(next).get("scanCursor").asText()).isEqualTo(cursor);
        assertThat(body(next).get("targetThroughSequence").asText()).isEqualTo("1");
        assertThat(body(next).get("items")).isEmpty();
    }
    @Test void malformedOrCrossOwnerCursorNeverCallsDatabase() throws Exception {
        String cursor = cursors.issue(new RecoveryCursor.Binding(new RecoveryCursor.Owner("prod", "billing"), RecoveryCursor.Kind.FEED, STREAM, null, 0, 2));
        assertThat(service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, cursor), OTHER).status()).isEqualTo(400);
        assertThat(service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, "bad"), OTHER).status()).isEqualTo(400);
        verifyNoInteractions(source);
    }
    @Test void gapHasRequiredNullRangeAndNoPartialItems() throws Exception {
        when(source.read("test", STREAM, 0, null)).thenReturn(observation(2, 2, 0, List.of(fact(1))));
        var reply = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, null), OTHER);
        assertThat(reply.status()).isEqualTo(409);
        JsonNode json = body(reply);
        assertThat(json.get("code").asText()).isEqualTo("COVERAGE_GAP");
        assertThat(json.get("details").get("unavailableRange").isNull()).isTrue();
        assertThat(json.has("items")).isFalse();
    }
    @Test void floorAndUncertainHaveDifferentErrorDetails() throws Exception {
        when(source.read("test", STREAM, 0, null)).thenReturn(observation(1, 1, 1, List.of()));
        var reply = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, null), OTHER);
        assertThat(reply.status()).isEqualTo(410);
        assertThat(body(reply).get("details").get("unavailableRange").isObject()).isTrue();
        when(source.read("test", STREAM, 0, null)).thenThrow(new MongoRecoveryReader.Unavailable());
        reply = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 0, null), OTHER);
        assertThat(reply.status()).isEqualTo(503); assertThat(body(reply).has("details")).isFalse();
    }
    @Test void maxCompletedDoesNotOverflowAndAfterHeadIsBadRequest() throws Exception {
        when(source.read("test", STREAM, Long.MAX_VALUE, null)).thenReturn(observation(Long.MAX_VALUE, Long.MAX_VALUE, 0, List.of()));
        var reply = service.page(owner, new RecoveryProtocol.FeedPage(STREAM, Long.MAX_VALUE, null), OTHER);
        assertThat(reply.status()).isEqualTo(200); assertThat(body(reply).get("done").asBoolean()).isTrue();
        when(source.read("test", STREAM, 3, null)).thenReturn(observation(2, 2, 0, List.of()));
        assertThat(service.page(owner, new RecoveryProtocol.FeedPage(STREAM, 3, null), OTHER).status()).isEqualTo(400);
    }
    @Test void replyDoesNotExposeMutableBufferOrPayloadInToString() {
        byte[] bytes = {1}; var reply = new RecoveryFeedService.Reply(200, bytes);
        bytes[0] = 2; reply.body()[0] = 3;
        assertThat(reply.body()).containsExactly((byte) 1);
        assertThat(reply.toString()).isEqualTo("RecoveryReply[status=200]");
    }
}
