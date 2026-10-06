package web.tosunsaeng.identity.domain.support;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

class SupportTransportTests {
    @Test void onlyCanonicalSlackEndpointsAllowed() {
        assertThat(SlackSupportNotifier.validateEndpoint("https://hooks.slack.com/services/TEST/TEST/FAKE").getHost()).isEqualTo("hooks.slack.com");
        for (String bad : List.of("http://hooks.slack.com/services/A/B/C", "https://example.com/services/A/B/C",
                "https://hooks.slack.com:443/services/A/B/C", "https://user@hooks.slack.com/services/A/B/C",
                "https://hooks.slack.com/services/A/B/C?secret=x", "https://hooks.slack.com/services/A/B/%2f")) {
            assertThatThrownBy(() -> SlackSupportNotifier.validateEndpoint(bad)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid support Slack endpoint");
        }
    }
    @Test void payloadUsesPlainTextAndNoSensitiveMetadata() throws Exception {
        var tree = new ObjectMapper().valueToTree(SlackSupportNotifier.payload("synthetic", "<!channel> test"));
        assertThat(tree.at("/blocks/0/text/type").asText()).isEqualTo("plain_text");
        assertThat(tree.at("/blocks/0/text/text").asText()).contains("<!channel>", "synthetic");
        assertThat(tree.get("text").asText()).doesNotContain("channel");
        assertThat(tree.get("unfurl_links").asBoolean()).isFalse();
        assertThat(tree.toString()).doesNotContain("userId", "replyEmail", "context");
    }
    @Test void retryAfterSupportsSecondsDateAndMalformed() {
        Instant now = Instant.parse("2026-10-06T00:00:00Z");
        assertThat(SlackSupportNotifier.retryAfter("120", now)).isEqualTo(Duration.ofSeconds(120));
        assertThat(SlackSupportNotifier.retryAfter("Tue, 6 Oct 2026 00:02:00 GMT", now)).isEqualTo(Duration.ofSeconds(120));
        assertThat(SlackSupportNotifier.retryAfter("invalid", now)).isEqualTo(Duration.ofSeconds(60));
    }
    @Test void ignoresForwardedHeaderFromUntrustedPeer() {
        assertThat(new SupportClientAddress(List.of()).resolve("192.0.2.1", "spoof")).isEqualTo("192.0.2.1");
    }
    @Test void walksTrustedChainFromRightAndStopsAtFirstUntrustedHop() {
        var addresses = new SupportClientAddress(List.of("10.0.0.0/8"));
        assertThat(addresses.resolve("10.0.0.1", "forged, 192.0.2.1, 10.0.0.2")).isEqualTo("192.0.2.1");
        assertThatThrownBy(() -> addresses.resolve("10.0.0.1", "example.com")).isInstanceOf(RuntimeException.class);
    }
    @Test void rejectsAmbiguousIPv4AndValidatesHmacSecret() {
        assertThatThrownBy(() -> SupportClientAddress.literal("127.1")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> SupportClientAddress.literal("127.000.0.1")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new SupportCrypto("invalid")).isInstanceOf(IllegalArgumentException.class);
        var crypto = new SupportCrypto(Base64.getEncoder().encodeToString(new byte[32]));
        assertThat(crypto.hash("user", "test")).isNotEqualTo(crypto.hash("ip", "test"));
    }
}
