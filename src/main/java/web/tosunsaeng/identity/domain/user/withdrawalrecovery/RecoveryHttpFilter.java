package web.tosunsaeng.identity.domain.user.withdrawalrecovery;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/** Terminal filter in a dedicated security chain. Not a servlet filter bean; no double registration. */
public final class RecoveryHttpFilter extends OncePerRequestFilter {
    private final RecoveryIngressVerifier verifier;
    private final RecoverySnapshotService snapshots;
    private final RecoveryFeedService feed;
    public RecoveryHttpFilter(RecoveryIngressVerifier verifier, RecoverySnapshotService snapshots, RecoveryFeedService feed) {
        this.verifier = Objects.requireNonNull(verifier);
        this.snapshots = Objects.requireNonNull(snapshots); this.feed = Objects.requireNonNull(feed);
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        String id = UUID.randomUUID().toString();
        RecoverySnapshotService.Reply reply;
        try {
            var route = Arrays.stream(RecoveryProtocol.Route.values())
                    .filter(r -> r.path().equals(request.getRequestURI().substring(request.getContextPath().length())))
                    .findFirst().orElse(null);
            if (route == null || !"POST".equals(request.getMethod())) { response.setStatus(403); return; }
            var owner = Objects.requireNonNull(verifier.authorizeHeaders(request, route));
            if (!validHeaders(request, route)) { writeError(response, RecoveryProtocol.Error.INVALID_REQUEST, id); return; }
            // Limit even chunked/false-length requests before handing bytes to any JSON decoder.
            byte[] bytes = request.getInputStream().readNBytes(RecoveryProtocol.MAX_BYTES + 1);
            if (bytes.length > RecoveryProtocol.MAX_BYTES) { writeError(response, RecoveryProtocol.Error.REQUEST_TOO_LARGE, id); return; }
            var decoded = RecoveryProtocol.decode(route, bytes, request.getHeader("Idempotency-Key"));
            reply = switch (decoded) {
                case RecoveryProtocol.Create value -> snapshots.create(owner, value, id);
                case RecoveryProtocol.Status value -> snapshots.status(owner, value, id);
                case RecoveryProtocol.SnapshotPage value -> snapshots.page(owner, value, id);
                case RecoveryProtocol.FeedPage value -> {
                    var result = feed.page(owner, value, id);
                    yield new RecoverySnapshotService.Reply(result.status(), result.body(), null);
                }
            };
        } catch (RecoveryIngressVerifier.Unauthenticated failure) { response.setStatus(401); return; }
        catch (SecurityException failure) { response.setStatus(403); return; }
        catch (RecoveryProtocol.Invalid failure) { writeError(response, failure.error(), id); return; }
        catch (RuntimeException failure) { writeError(response, RecoveryProtocol.Error.SOURCE_UNAVAILABLE, id); return; }
        // Services serialize completely; check again before committing any HTTP bytes.
        byte[] body = reply.body();
        if (body.length > RecoveryProtocol.MAX_BYTES) { writeError(response, RecoveryProtocol.Error.RESPONSE_LIMIT_EXCEEDED, id); return; }
        if (reply.retryAfterSeconds() != null) response.setHeader("Retry-After", reply.retryAfterSeconds().toString());
        write(response, reply.status(), body);
    }
    private boolean validHeaders(HttpServletRequest request, RecoveryProtocol.Route route) {
        if (request.getQueryString() != null || request.getHeader("Content-Encoding") != null) return false;
        if (Collections.list(request.getHeaders("Content-Type")).size() != 1) return false;
        int keys = Collections.list(request.getHeaders("Idempotency-Key")).size();
        if (route == RecoveryProtocol.Route.CREATE ? keys != 1 : keys != 0) return false;
        try {
            var type = MediaType.parseMediaType(request.getContentType());
            return "application".equals(type.getType()) && "json".equals(type.getSubtype())
                    && type.getParameters().keySet().stream().allMatch("charset"::equalsIgnoreCase)
                    && (type.getCharset() == null || StandardCharsets.UTF_8.equals(type.getCharset()));
        } catch (IllegalArgumentException failure) { return false; }
    }
    private void writeError(HttpServletResponse response, RecoveryProtocol.Error error, String id) throws IOException {
        write(response, error.status(), RecoveryProtocol.encode(RecoveryProtocol.errorBody(error, id)));
    }
    private void write(HttpServletResponse response, int status, byte[] body) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8"); response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }
}
