package web.tosunsaeng.identity.domain.support;

import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SlackSupportNotifier implements SupportNotifier {
    private final URI endpoint;
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Clock clock;
    public SlackSupportNotifier(String endpoint, ObjectMapper mapper, Clock clock) {
        this.endpoint = validateEndpoint(endpoint); this.mapper = mapper; this.clock = clock;
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).followRedirects(HttpClient.Redirect.NEVER).build();
    }
    static URI validateEndpoint(String value) {
        URI uri;
        try { uri = URI.create(value == null ? "" : value); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid support Slack endpoint"); }
        if (!"https".equals(uri.getScheme()) || !"hooks.slack.com".equals(uri.getHost()) || uri.getPort() != -1
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !uri.getPath().matches("/services/[A-Za-z0-9]+/[A-Za-z0-9]+/[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Invalid support Slack endpoint");
        }
        return uri;
    }
    static Map<String, Object> payload(String id, String message) {
        // No top-level untrusted fallback text. plain_text blocks do not parse user-controlled mentions.
        return Map.of("text", "문의가 접수되었습니다.", "unfurl_links", false, "unfurl_media", false,
                "blocks", List.of(Map.of("type", "section", "text", Map.of("type", "plain_text", "emoji", false,
                        "text", "[문의 접수번호: " + id + "]\n\n" + message))));
    }
    public Outcome send(String id, String message) {
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload(id, message)))).build();
            // Headers only: do not retain/log Slack's response body or URLs. Standard incoming webhook success is 200.
            var response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            if (status == 200) return new Outcome(true, false, Duration.ZERO, "OK");
            boolean retry = status == 408 || status == 429 || status >= 500;
            Duration wait = status == 429 ? retryAfter(response.headers().firstValue("Retry-After").orElse(""), clock.instant()) : Duration.ZERO;
            return new Outcome(false, retry, wait, status == 429 ? "RATE_LIMIT" : retry ? "REMOTE_TRANSIENT" : "REMOTE_REJECTED");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return new Outcome(false, true, Duration.ZERO, "INTERRUPTED");
        } catch (Exception e) { return new Outcome(false, true, Duration.ZERO, "TRANSPORT"); }
    }
    static Duration retryAfter(String value, Instant now) {
        try {
            long seconds = Long.parseLong(value);
            return Duration.ofSeconds(Math.max(0, seconds));
        } catch (RuntimeException e) {
            try { return Duration.ofSeconds(Math.max(0, ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond() - now.getEpochSecond())); }
            catch (RuntimeException ignored) { return Duration.ofSeconds(60); }
        }
    }
    @Override public String toString() { return "SlackSupportNotifier[endpoint=REDACTED]"; }
}
