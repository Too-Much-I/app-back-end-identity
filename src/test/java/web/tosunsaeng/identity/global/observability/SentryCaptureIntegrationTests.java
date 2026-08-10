package web.tosunsaeng.identity.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.Breadcrumb;
import io.sentry.EventProcessor;
import io.sentry.Hint;
import io.sentry.ITransportFactory;
import io.sentry.RequestDetails;
import io.sentry.Sentry;
import io.sentry.SentryEnvelope;
import io.sentry.SentryEnvelopeItem;
import io.sentry.SentryEvent;
import io.sentry.SentryItemType;
import io.sentry.SentryOptions;
import io.sentry.protocol.DebugImage;
import io.sentry.protocol.DebugMeta;
import io.sentry.protocol.Mechanism;
import io.sentry.protocol.Message;
import io.sentry.protocol.Request;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.protocol.SentryThread;
import io.sentry.protocol.User;
import io.sentry.transport.ITransport;
import io.sentry.transport.RateLimiter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import web.tosunsaeng.identity.domain.auth.domain.repository.RefreshSessionRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.global.exception.BusinessException;
import web.tosunsaeng.identity.global.exception.CommonErrorStatus;
import web.tosunsaeng.identity.global.security.jwt.TestRsaKeyConfiguration;
import web.tosunsaeng.identity.support.LogCapture;

@SpringBootTest(properties = {
		"sentry.enabled=true",
		"sentry.dsn=https://public@example.invalid/1",
		"sentry.environment=sentry-test",
		"sentry.release=identity-test",
		// Test에서는 SDK가 최대한 많은 원본 문맥을 붙이게 해 beforeSend 방어선을 검증한다.
		"sentry.send-default-pii=true",
		"sentry.max-request-body-size=always",
		"sentry.attach-server-name=true",
		"sentry.send-modules=true",
		"sentry.traces-sample-rate=0.0",
		"sentry.profiles-sample-rate=0.0",
		"sentry.logging.enabled=false",
		"sentry.logs.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({
		TestRsaKeyConfiguration.class,
		SentryCaptureIntegrationTests.SentryTestConfiguration.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SentryCaptureIntegrationTests {

	private static final String SENSITIVE_SENTINEL =
			"TEST_ONLY_SENSITIVE_SENTINEL_7C0F5E6A";
	private static final String SAFE_REQUEST_ID = "sentry-request-123";
	private static final String SAFE_DEBUG_ID = "12345678-1234-1234-1234-123456789abc";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RecordingSentryTransportFactory transportFactory;

	@Autowired
	private ApplicationContext applicationContext;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private RefreshSessionRepository refreshSessionRepository;

	@BeforeEach
	void clearCapturedEvents() {
		transportFactory.clear();
	}

	@AfterAll
	static void closeSentry() {
		Sentry.close();
	}

	@Test
	void expectedBusiness4xxDoesNotCreateSentryEvent() throws Exception {
		mockMvc.perform(post("/test/sentry/business")
					.with(jwt().jwt(token -> token.subject(SENSITIVE_SENTINEL)))
					.queryParam("credential", SENSITIVE_SENTINEL)
					.header("X-Test-Sensitive", SENSITIVE_SENTINEL)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"password\":\"" + SENSITIVE_SENTINEL + "\"}"))
				.andExpect(status().isBadRequest());

		Sentry.flush(1_000);
		assertThat(transportFactory.eventPayloads()).isEmpty();
	}

	@Test
	void handledUnexpected5xxCreatesExactlyOneSanitizedFinalEvent() throws Exception {
		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			mockMvc.perform(post("/test/sentry/unexpected")
						.with(jwt().jwt(token -> token.subject(SENSITIVE_SENTINEL)))
						.header(RequestLoggingFilter.REQUEST_ID_HEADER, SAFE_REQUEST_ID)
						.header("X-Test-Sensitive", SENSITIVE_SENTINEL)
						.header("Cookie", "session=" + SENSITIVE_SENTINEL)
						.queryParam("credential", SENSITIVE_SENTINEL)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"refreshToken\":\"" + SENSITIVE_SENTINEL + "\"}"))
					.andExpect(status().isInternalServerError());

			Sentry.flush(1_000);

			// Handler의 명시 capture 뒤 ERROR 로그가 발생해도 Sentry Issue는 한 건뿐이다.
			assertThat(logs.events("http.request.failed")).hasSize(1);
			assertThat(transportFactory.eventPayloads()).singleElement()
					.satisfies(this::assertSanitizedFinalEvent);
		}
	}

	@Test
	void sentryLoggingIntegrationIsNotInstalled() {
		assertThat(applicationContext.containsBean("sentryLogbackInitializer")).isFalse();
	}

	private void assertSanitizedFinalEvent(String payload) {
		assertThat(payload).doesNotContain(SENSITIVE_SENTINEL);

		try {
			JsonNode event = objectMapper.readTree(payload);
			assertThat(event.path("request").isMissingNode()).isTrue();
			assertThat(event.path("user").isMissingNode()).isTrue();
			assertThat(event.path("message").isMissingNode()).isTrue();
			assertThat(event.path("breadcrumbs").isMissingNode()).isTrue();
			assertThat(event.path("extra").isMissingNode()).isTrue();
			assertThat(event.path("contexts").isEmpty()).isTrue();
			assertThat(event.path("environment").asText()).isEqualTo("sentry-test");
			assertThat(event.path("release").asText()).isEqualTo("identity-test");
			assertThat(event.path("platform").asText()).isEqualTo("java");
			assertThat(event.at("/tags/requestId").asText()).isEqualTo(SAFE_REQUEST_ID);
			assertThat(event.at("/tags/errorCode").asText())
					.isEqualTo("INTERNAL_SERVER_ERROR");
			assertThat(event.at("/tags/http.method").asText()).isEqualTo("POST");
			assertThat(event.at("/tags/http.route").asText())
					.isEqualTo("/test/sentry/unexpected");
			assertThat(event.at("/tags/http.status_code").asText()).isEqualTo("500");
			assertThat(event.at("/exception/values")).hasSize(1);
			assertThat(event.at("/exception/values/0/type").asText())
					.isEqualTo(IllegalStateException.class.getSimpleName());
			assertThat(event.at("/exception/values/0/value").isMissingNode()).isTrue();
			assertThat(event.at("/exception/values/0/stacktrace/frames").isArray()).isTrue();
			assertThat(event.at("/debug_meta/images")).hasSize(1);
			assertThat(event.at("/debug_meta/images/0/type").asText()).isEqualTo("jvm");
			assertThat(event.at("/debug_meta/images/0/debug_id").asText())
					.isEqualTo(SAFE_DEBUG_ID);
		} catch (IOException exception) {
			throw new AssertionError("Sentry event JSON을 읽을 수 없습니다.", exception);
		}
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class SentryTestConfiguration {

		@Bean
		RecordingSentryTransportFactory recordingSentryTransportFactory() {
			return new RecordingSentryTransportFactory();
		}

		@Bean
		EventProcessor sensitiveEventContaminatingProcessor() {
			return new SensitiveEventContaminatingProcessor();
		}

		@Bean
		SentryTestController sentryTestController() {
			return new SentryTestController();
		}
	}

	@RestController
	static class SentryTestController {

		@PostMapping("/test/sentry/business")
		void business(@RequestBody Map<String, Object> ignored) {
			throw new BusinessException(CommonErrorStatus.INVALID_REQUEST);
		}

		@PostMapping("/test/sentry/unexpected")
		void unexpected(@RequestBody Map<String, Object> ignored) {
			throw new IllegalStateException(SENSITIVE_SENTINEL);
		}
	}

	static final class SensitiveEventContaminatingProcessor implements EventProcessor {

		@Override
		public SentryEvent process(SentryEvent event, Hint hint) {
			Message message = new Message();
			message.setMessage(SENSITIVE_SENTINEL);
			message.setFormatted(SENSITIVE_SENTINEL);
			message.setParams(List.of(SENSITIVE_SENTINEL));
			event.setMessage(message);
			event.setLogger(SENSITIVE_SENTINEL);
			event.setTransaction(SENSITIVE_SENTINEL);
			event.setEnvironment(SENSITIVE_SENTINEL);
			event.setRelease(SENSITIVE_SENTINEL);
			event.setPlatform(SENSITIVE_SENTINEL);
			event.setServerName(SENSITIVE_SENTINEL);
			event.setDist(SENSITIVE_SENTINEL);
			event.setFingerprints(List.of(SENSITIVE_SENTINEL));
			event.setModule("test-module", SENSITIVE_SENTINEL);
			event.setTag("test.sensitive", SENSITIVE_SENTINEL);
			event.setExtra("test.sensitive", SENSITIVE_SENTINEL);
			event.addBreadcrumb(new Breadcrumb(SENSITIVE_SENTINEL));
			event.getContexts().put("test-sensitive", Map.of("value", SENSITIVE_SENTINEL));
			event.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));
			event.setDebugMeta(contaminatedDebugMeta());

			Request request = new Request();
			request.setUrl("https://example.invalid/" + SENSITIVE_SENTINEL);
			request.setMethod(SENSITIVE_SENTINEL);
			request.setQueryString("credential=" + SENSITIVE_SENTINEL);
			request.setData(Map.of("password", SENSITIVE_SENTINEL));
			request.setCookies("session=" + SENSITIVE_SENTINEL);
			request.setHeaders(Map.of("Authorization", SENSITIVE_SENTINEL));
			request.setEnvs(Map.of("SECRET", SENSITIVE_SENTINEL));
			request.setOthers(Map.of("secret", SENSITIVE_SENTINEL));
			request.setFragment(SENSITIVE_SENTINEL);
			request.setApiTarget(SENSITIVE_SENTINEL);
			event.setRequest(request);

			User user = new User();
			user.setId(SENSITIVE_SENTINEL);
			user.setEmail(SENSITIVE_SENTINEL);
			user.setUsername(SENSITIVE_SENTINEL);
			user.setIpAddress(SENSITIVE_SENTINEL);
			user.setData(Map.of("secret", SENSITIVE_SENTINEL));
			event.setUser(user);

			SentryThread thread = new SentryThread();
			thread.setName(SENSITIVE_SENTINEL);
			event.setThreads(List.of(thread));

			if (event.getExceptions() != null) {
				for (SentryException exception : event.getExceptions()) {
					contaminateException(exception);
				}
			}
			return event;
		}

		private DebugMeta contaminatedDebugMeta() {
			DebugImage safeBundle = new DebugImage();
			safeBundle.setType(DebugImage.JVM);
			safeBundle.setDebugId(SAFE_DEBUG_ID);
			safeBundle.setCodeFile(SENSITIVE_SENTINEL);
			safeBundle.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));

			DebugImage contaminated = new DebugImage();
			contaminated.setType(SENSITIVE_SENTINEL);
			contaminated.setDebugId(SENSITIVE_SENTINEL);

			DebugMeta debugMeta = new DebugMeta();
			debugMeta.setImages(List.of(safeBundle, contaminated));
			debugMeta.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));
			return debugMeta;
		}

		private void contaminateException(SentryException exception) {
			exception.setValue(SENSITIVE_SENTINEL);
			exception.setModule("invalid-module-" + SENSITIVE_SENTINEL);
			exception.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));

			Mechanism mechanism = new Mechanism();
			mechanism.setDescription(SENSITIVE_SENTINEL);
			mechanism.setData(Map.of("secret", SENSITIVE_SENTINEL));
			mechanism.setMeta(Map.of("secret", SENSITIVE_SENTINEL));
			mechanism.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));
			exception.setMechanism(mechanism);

			SentryStackTrace stackTrace = exception.getStacktrace();
			if (stackTrace == null || stackTrace.getFrames() == null) {
				return;
			}
			stackTrace.setRegisters(Map.of("secret", SENSITIVE_SENTINEL));
			stackTrace.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));
			for (SentryStackFrame frame : stackTrace.getFrames()) {
				frame.setAbsPath("/private/" + SENSITIVE_SENTINEL);
				frame.setContextLine(SENSITIVE_SENTINEL);
				frame.setPreContext(List.of(SENSITIVE_SENTINEL));
				frame.setPostContext(List.of(SENSITIVE_SENTINEL));
				frame.setVars(Map.of("secret", SENSITIVE_SENTINEL));
				frame.setPackage(SENSITIVE_SENTINEL);
				frame.setRawFunction(SENSITIVE_SENTINEL);
				frame.setSymbol(SENSITIVE_SENTINEL);
				frame.setUnknown(Map.of("test_sensitive", SENSITIVE_SENTINEL));
			}
		}
	}

	static final class RecordingSentryTransportFactory implements ITransportFactory {

		private final List<String> eventPayloads = new CopyOnWriteArrayList<>();

		@Override
		public ITransport create(SentryOptions options, RequestDetails requestDetails) {
			return new RecordingSentryTransport(options, eventPayloads);
		}

		void clear() {
			eventPayloads.clear();
		}

		List<String> eventPayloads() {
			return List.copyOf(eventPayloads);
		}
	}

	static final class RecordingSentryTransport implements ITransport {

		private final RateLimiter rateLimiter;
		private final List<String> eventPayloads;

		RecordingSentryTransport(SentryOptions options, List<String> eventPayloads) {
			this.rateLimiter = new RateLimiter(options);
			this.eventPayloads = eventPayloads;
		}

		@Override
		public void send(SentryEnvelope envelope, Hint hint) throws IOException {
			for (SentryEnvelopeItem item : envelope.getItems()) {
				if (item.getHeader().getType() != SentryItemType.Event) {
					continue;
				}
				try {
					eventPayloads.add(new String(item.getData(), StandardCharsets.UTF_8));
				} catch (Exception exception) {
					throw new IOException("Sentry test event를 읽을 수 없습니다.", exception);
				}
			}
		}

		@Override
		public void flush(long timeoutMillis) {
		}

		@Override
		public RateLimiter getRateLimiter() {
			return rateLimiter;
		}

		@Override
		public void close() {
		}

		@Override
		public void close(boolean isRestarting) {
		}
	}
}
