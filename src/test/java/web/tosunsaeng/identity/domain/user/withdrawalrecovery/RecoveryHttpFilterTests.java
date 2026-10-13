package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;

class RecoveryHttpFilterTests {
    static final String ID = "00000000-0000-4000-8000-000000000001";
    final RecoveryIngressVerifier verifier = mock(RecoveryIngressVerifier.class);
    final RecoverySnapshotService snapshots = mock(RecoverySnapshotService.class);
    final RecoveryFeedService feed = mock(RecoveryFeedService.class);
    final RecoveryCursor.Owner owner = new RecoveryCursor.Owner("test", "billing");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final MockFilterChain chain = new MockFilterChain();
    final RecoveryHttpFilter filter = new RecoveryHttpFilter(verifier, snapshots, feed);
    @BeforeEach void setup() {
        when(verifier.authorizeHeaders(any(), any())).thenReturn(owner);
        when(snapshots.create(eq(owner), any(), any())).thenReturn(new RecoverySnapshotService.Reply(202, "{}".getBytes(StandardCharsets.UTF_8), 5));
    }
    MockHttpServletRequest request() {
        var r = new MockHttpServletRequest("POST", RecoveryProtocol.Route.CREATE.path());
        r.setContentType("application/json"); r.addHeader("Idempotency-Key", ID);
        r.setContent(("{\"streamId\":\"" + ID + "\"}").getBytes(StandardCharsets.UTF_8)); return r;
    }
    void run(MockHttpServletRequest r) throws Exception { filter.doFilter(r, response, chain); }
    @Test void authenticatedCreateUsesVerifiedOwnerAndPreservesRetryAfter() throws Exception {
        var r = request(); r.addHeader("X-Consumer", "attacker"); run(r);
        assertThat(response.getStatus()).isEqualTo(202); assertThat(response.getHeader("Retry-After")).isEqualTo("5");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store"); assertThat(chain.getRequest()).isNull();
        var order = inOrder(verifier, snapshots);
        order.verify(verifier).authorizeHeaders(eq(r), eq(RecoveryProtocol.Route.CREATE));
        order.verify(snapshots).create(eq(owner), eq(new RecoveryProtocol.Create(ID, ID)), anyString());
    }
    @Test void rejectedHeadersNeverReadOrDecodeBody() throws Exception {
        when(verifier.authorizeHeaders(any(), any())).thenThrow(new SecurityException());
        run(request()); assertThat(response.getStatus()).isEqualTo(403); verifyNoInteractions(snapshots, feed);
    }
    @Test void rejectedSignatureNeverCallsService() throws Exception {
        when(verifier.authorizeHeaders(any(), any())).thenThrow(new RecoveryIngressVerifier.Unauthenticated()); run(request());
        assertThat(response.getStatus()).isEqualTo(401); verifyNoInteractions(snapshots, feed);
    }
    @Test void duplicateIdempotencyHeadersAreRejected() throws Exception {
        var r = request(); r.addHeader("Idempotency-Key", ID); run(r);
        assertThat(response.getStatus()).isEqualTo(400); verifyNoInteractions(snapshots);
    }
    @Test void unsupportedEncodingIsRejected() throws Exception {
        var r = request(); r.addHeader("Content-Encoding", "gzip"); run(r);
        assertThat(response.getStatus()).isEqualTo(400); verifyNoInteractions(snapshots);
    }
    @Test void nonUtf8CharsetIsRejected() throws Exception {
        var r = request(); r.setContentType("application/json;charset=ISO-8859-1"); run(r);
        assertThat(response.getStatus()).isEqualTo(400);
    }
    @Test void bodyLimitIsEnforcedBeforePayloadDecoder() throws Exception {
        var r = request(); r.setContent(new byte[RecoveryProtocol.MAX_BYTES + 1]); run(r);
        assertThat(response.getStatus()).isEqualTo(413); verifyNoInteractions(snapshots);
        assertThat(response.getContentAsString()).contains("REQUEST_TOO_LARGE");
    }
    @Test void exactByteLimitStillAcceptsValidJson() throws Exception {
        var r = request(); byte[] body = java.util.Arrays.copyOf(r.getContentAsByteArray(), RecoveryProtocol.MAX_BYTES);
        java.util.Arrays.fill(body, r.getContentAsByteArray().length, body.length, (byte) ' '); r.setContent(body); run(r);
        assertThat(response.getStatus()).isEqualTo(202);
    }
    @Test void duplicateContentTypeIsRejected() throws Exception {
        var r = request(); r.addHeader("Content-Type", "application/json"); run(r);
        assertThat(response.getStatus()).isEqualTo(400); verifyNoInteractions(snapshots);
    }
    @Test void invalidUtf8NeverReachesService() throws Exception {
        var r = request(); r.setContent(new byte[]{(byte) 0xc3, 0x28}); run(r);
        assertThat(response.getStatus()).isEqualTo(400); verifyNoInteractions(snapshots);
    }
    @Test void duplicateJsonIsRejectedAfterAuthentication() throws Exception {
        var r = request(); r.setContent(("{\"streamId\":\"" + ID + "\",\"streamId\":\"" + ID + "\"}").getBytes(StandardCharsets.UTF_8)); run(r);
        assertThat(response.getStatus()).isEqualTo(400); verify(verifier).authorizeHeaders(any(), any()); verifyNoInteractions(snapshots);
    }
    @Test void wrongMethodNeverFallsThrough() throws Exception {
        var r = request(); r.setMethod("GET"); run(r); assertThat(response.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull(); verifyNoInteractions(verifier, snapshots);
    }
    @Test void queryParametersAreNotAlternativeIdentityInput() throws Exception {
        var r = request(); r.setQueryString("consumer=attacker"); run(r); assertThat(response.getStatus()).isEqualTo(400);
    }
    @Test void oversizedResponseIsNotPartiallyWritten() throws Exception {
        when(snapshots.create(any(), any(), any())).thenReturn(new RecoverySnapshotService.Reply(202, new byte[16385], 5)); run(request());
        assertThat(response.getStatus()).isEqualTo(500); assertThat(response.getContentAsString()).contains("RESPONSE_LIMIT_EXCEEDED");
        assertThat(response.getHeader("Retry-After")).isNull();
    }
    @Test void runtimeFailureNeverLeaksExceptionText() throws Exception {
        when(snapshots.create(any(), any(), any())).thenThrow(new IllegalStateException("synthetic-private-detail")); run(request());
        assertThat(response.getStatus()).isEqualTo(503); assertThat(response.getContentAsString()).doesNotContain("synthetic-private-detail");
    }
    @Test void remainingThreeRoutesDispatchToTheirOwnService() throws Exception {
        for (var route : new RecoveryProtocol.Route[]{RecoveryProtocol.Route.STATUS, RecoveryProtocol.Route.SNAPSHOT_PAGE, RecoveryProtocol.Route.FEED_PAGE}) {
            var r = new MockHttpServletRequest("POST", route.path()); r.setContentType("application/json");
            String body = switch (route) {
                case STATUS -> "{\"snapshotId\":\"" + ID + "\"}";
                case SNAPSHOT_PAGE -> "{\"snapshotId\":\"" + ID + "\",\"cursor\":null}";
                default -> "{\"streamId\":\"" + ID + "\",\"afterSequence\":\"0\",\"scanCursor\":null}";
            };
            r.setContent(body.getBytes(StandardCharsets.UTF_8));
            when(snapshots.status(any(), any(), any())).thenReturn(new RecoverySnapshotService.Reply(200, "{}".getBytes(), null));
            when(snapshots.page(any(), any(), any())).thenReturn(new RecoverySnapshotService.Reply(200, "{}".getBytes(), null));
            when(feed.page(any(), any(), any())).thenReturn(new RecoveryFeedService.Reply(200, "{}".getBytes()));
            var out = new MockHttpServletResponse(); filter.doFilter(r, out, new MockFilterChain()); assertThat(out.getStatus()).isEqualTo(200);
        }
        verify(snapshots).status(eq(owner), any(), any()); verify(snapshots).page(eq(owner), any(), any()); verify(feed).page(eq(owner), any(), any());
    }
}
