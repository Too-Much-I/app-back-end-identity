package web.tosunsaeng.identity.global.workload;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class BillingJwtJsonTransportTests {
    static RSAKey key;
    static final Clock CLOCK = Clock.fixed(Instant.ofEpochSecond(1000), ZoneOffset.UTC);
    @BeforeAll static void keys() throws Exception { key = new RSAKeyGenerator(2048).keyID("fixture-outbound").generate(); }
    Map<String,String> audiences() {
        var map = new HashMap<String,String>();
        BillingJwtJsonTransport.ROUTES.forEach(r -> map.put(r, "fixture:aud:" + r)); return map;
    }
    BillingJwtJsonTransport.Factory factory() { return new BillingJwtJsonTransport.Factory(key.toJSONString(), "fixture:identity:test", audiences(), CLOCK); }
    @Test void eachAttemptUsesFreshJwtWithPurposeAudienceAndUnchangedBody() throws Exception {
        HttpClient client = mock(HttpClient.class); when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
        HttpResponse<Void> response = mock(HttpResponse.class); when(response.statusCode()).thenReturn(204);
        when(response.headers()).thenReturn(HttpHeaders.of(Map.of("Retry-After", List.of("30")), (a,b) -> true));
        when(client.<Void>send(any(), any())).thenReturn(response);
        var transport = new BillingJwtJsonTransport(factory(), URI.create("https://billing.invalid"), Duration.ofSeconds(5), client);
        byte[] body = "{\"eventId\":\"synthetic\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (String route : BillingJwtJsonTransport.ROUTES) {
            assertThat(transport.post(route, body)).isEqualTo(new WorkloadDeliveryResult(204, 30));
            assertThat(transport.postDetailed(route, body).status()).isEqualTo(204);
        }
        var requests = ArgumentCaptor.forClass(HttpRequest.class); verify(client, times(8)).send(requests.capture(), any());
        var ids = new HashSet<String>();
        for (var request : requests.getAllValues()) {
            String route = request.uri().getPath();
            assertThat(request.uri().getHost()).isEqualTo("billing.invalid"); assertThat(request.method()).isEqualTo("POST");
            assertThat(readBody(request)).isEqualTo(body);
            assertThat(request.headers().firstValue("X-Amz-Date")).isEmpty();
            assertThat(request.headers().firstValue("traceparent").orElseThrow()).matches("00-[0-9a-f]{32}-[0-9a-f]{16}-01");
            var jwt = JWSObject.parse(request.headers().firstValue("Authorization").orElseThrow().substring(7));
            assertThat(jwt.verify(new RSASSAVerifier(key.toPublicJWK()))).isTrue();
            var claims = jwt.getPayload().toJSONObject();
            assertThat(claims).containsEntry("iss", "fixture:identity:test").containsEntry("sub", "identity-service")
                    .containsEntry("aud", audiences().get(route));
            assertThat(((Number)claims.get("iat")).longValue()).isEqualTo(1000);
            assertThat(((Number)claims.get("exp")).longValue()).isEqualTo(1060);
            assertThat(ids.add((String)claims.get("jti"))).isTrue();
        }
    }
    @Test void wrongRouteAndOriginAndRedirectsCannotLeakCredential() {
        var client = mock(HttpClient.class); when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
        var transport = new BillingJwtJsonTransport(factory(), URI.create("https://billing.invalid"), Duration.ofSeconds(5), client);
        assertThatThrownBy(() -> transport.post("/other", new byte[0])).isInstanceOf(IllegalArgumentException.class);
        for (String origin : List.of("http://billing.invalid", "https://user@billing.invalid", "https://billing.invalid/path", "https://billing.invalid?q=1"))
            assertThatThrownBy(() -> new BillingJwtJsonTransport(factory(), URI.create(origin), Duration.ofSeconds(5), client))
                    .isInstanceOf(IllegalArgumentException.class);
        when(client.followRedirects()).thenReturn(HttpClient.Redirect.ALWAYS);
        assertThatThrownBy(() -> new BillingJwtJsonTransport(factory(), URI.create("https://billing.invalid"), Duration.ofSeconds(5), client))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void invalidKeyOrPurposeMapFailsWithoutInputInException() {
        assertThatThrownBy(() -> new BillingJwtJsonTransport.Factory(key.toPublicJWK().toJSONString(), "issuer", audiences(), CLOCK))
                .hasMessage("Invalid Billing workload signing configuration.").hasNoCause();
        assertThatThrownBy(() -> new BillingJwtJsonTransport.Factory(key.toJSONString(), "issuer", Map.of(), CLOCK))
                .hasMessage("Invalid Billing workload signing configuration.").hasNoCause();
    }
    @Test void interruptedSendRestoresInterruptAndDoesNotFallback() throws Exception {
        var client = mock(HttpClient.class); when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
        when(client.send(any(), any())).thenThrow(new InterruptedException("synthetic"));
        var transport = new BillingJwtJsonTransport(factory(), URI.create("https://billing.invalid"), Duration.ofSeconds(5), client);
        try {
            assertThatThrownBy(() -> transport.post(BillingJwtJsonTransport.ROUTES.iterator().next(), new byte[0]))
                    .isInstanceOf(BillingJsonTransport.TransportException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue(); verify(client, times(1)).send(any(), any());
        } finally { Thread.interrupted(); }
    }
    @Test void jwtFactoryIsAbsentByDefaultAndInvalidEnabledSetupFailsStartup() {
        var runner = new ApplicationContextRunner().withUserConfiguration(BillingJwtConfiguration.class);
        runner.run(c -> { assertThat(c).hasNotFailed(); assertThat(c).doesNotHaveBean(BillingJwtJsonTransport.Factory.class); });
        runner.withPropertyValues("app.withdrawal-capture.publisher-enabled=true").run(c -> assertThat(c).hasFailed());
    }
    private static byte[] readBody(HttpRequest request) {
        var result = new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(java.nio.ByteBuffer b) { byte[] bytes = new byte[b.remaining()]; b.get(bytes); result.writeBytes(bytes); }
            public void onError(Throwable t) { throw new AssertionError(t); }
            public void onComplete() { }
        });
        return result.toByteArray();
    }
}
