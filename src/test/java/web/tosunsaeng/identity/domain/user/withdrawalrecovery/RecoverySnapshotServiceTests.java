package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static web.tosunsaeng.identity.domain.user.withdrawalrecovery.RecoveryProtocolTests.fixture;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RecoverySnapshotServiceTests {
    static final String ID = "00000000-0000-4000-8000-000000000001";
    static final String OTHER = "00000000-0000-4000-8000-000000000002";
    final RecoveryCursor.Owner owner = new RecoveryCursor.Owner("test", "billing");
    final MongoSnapshotStore store = mock(MongoSnapshotStore.class);
    final RecoveryCursor cursor = new RecoveryCursor(Map.of("test", new byte[32]), "test", Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    final RecoverySnapshotService service = new RecoverySnapshotService(store, cursor);
    JsonNode json(RecoverySnapshotService.Reply reply) throws Exception { return new ObjectMapper().readTree(reply.body()); }
    void schema(String name, JsonNode body) throws Exception {
        ObjectNode root = (ObjectNode) fixture("wire.schema.json"); root.put("$ref", "#/$defs/" + name);
        assertThat(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(root).validate(body)).isEmpty();
    }
    @Test void buildingCreate202AndStatus200BothRetryAfter5() throws Exception {
        var view = new MongoSnapshotStore.View(ID, ID, "BUILDING", null, null);
        var request = new RecoveryProtocol.Create(ID, OTHER);
        when(store.create(owner, request)).thenReturn(view); when(store.status(owner, ID)).thenReturn(view);
        var create = service.create(owner, request, OTHER); var status = service.status(owner, new RecoveryProtocol.Status(ID), OTHER);
        assertThat(create.status()).isEqualTo(202); assertThat(status.status()).isEqualTo(200);
        assertThat(create.retryAfterSeconds()).isEqualTo(5); assertThat(status.retryAfterSeconds()).isEqualTo(5);
        schema("SnapshotStatus", json(create)); schema("SnapshotStatus", json(status));
        assertThat(json(create).has("manifest")).isFalse(); assertThat(json(create).has("failureCode")).isFalse();
    }
    @ParameterizedTest @CsvSource({"BUILDING,SNAPSHOT_NOT_READY,409", "FAILED,SNAPSHOT_FAILED,409", "EXPIRED,SNAPSHOT_EXPIRED,410"})
    void statePrecedesInvalidCursor(String state, String code, int status) throws Exception {
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, state, null, state.equals("FAILED") ? "SNAPSHOT_BUILD_FAILED" : null));
        var reply = service.page(owner, new RecoveryProtocol.SnapshotPage(ID, "invalid"), OTHER);
        assertThat(reply.status()).isEqualTo(status); assertThat(json(reply).get("code").asText()).isEqualTo(code);
        schema("Error", json(reply));
        verify(store, never()).page(any(), any(), any(), anyLong(), anyLong(), any());
    }
    @Test void finalPageHasNullCursorAndExactShape() throws Exception {
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, "READY", null, null));
        when(store.page(owner, ID, ID, 0, -1, null)).thenReturn(new MongoSnapshotStore.Page(List.of(new SnapshotDigest.Item(OTHER, Instant.EPOCH)), true, OTHER, 1));
        var reply = service.page(owner, new RecoveryProtocol.SnapshotPage(ID, null), OTHER);
        assertThat(reply.status()).isEqualTo(200); assertThat(json(reply).get("nextCursor").isNull()).isTrue();
        schema("SnapshotPage", json(reply));
    }
    @Test void failedStatusOnlyHasFailureCodeExpiredOmitsIt() throws Exception {
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, "FAILED", null, "SNAPSHOT_HISTORY_LOST"));
        var failed = service.status(owner, new RecoveryProtocol.Status(ID), OTHER);
        schema("SnapshotStatus", json(failed)); assertThat(failed.retryAfterSeconds()).isNull();
        when(store.status(owner, ID)).thenReturn(new MongoSnapshotStore.View(ID, ID, "EXPIRED", null, null));
        var expired = json(service.status(owner, new RecoveryProtocol.Status(ID), OTHER));
        schema("SnapshotStatus", expired); assertThat(expired.has("failureCode")).isFalse();
    }
    @Test void databaseFailureGetsSanitized503NotRawCause() throws Exception {
        when(store.status(owner, ID)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("sensitive diagnostic"));
        var reply = service.status(owner, new RecoveryProtocol.Status(ID), OTHER);
        assertThat(reply.status()).isEqualTo(503); assertThat(json(reply).toString()).doesNotContain("sensitive diagnostic"); schema("Error", json(reply));
    }
    @Test void replyAndViewAreDefensiveAndRedacted() {
        byte[] bytes = {1}; var reply = new RecoverySnapshotService.Reply(200, bytes, null); bytes[0] = 2; reply.body()[0] = 3;
        assertThat(reply.body()).containsExactly((byte) 1);
        var node = new ObjectMapper().createObjectNode().put("x", "before");
        var view = new MongoSnapshotStore.View(ID, ID, "READY", node, null);
        node.put("x", "after"); view.manifest().put("x", "other");
        assertThat(view.manifest().get("x").asText()).isEqualTo("before");
        assertThat(view.toString() + reply).doesNotContain(ID, "before");
    }
}
