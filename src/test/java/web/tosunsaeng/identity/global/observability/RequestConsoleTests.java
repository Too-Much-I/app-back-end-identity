package web.tosunsaeng.identity.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.status.Status;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.dao.QueryTimeoutException;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthErrorStatus;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;

/** Production ECS encoder + real ConsoleAppender, isolated without resetting global logging. */
@ResourceLock("SYSTEM_OUT")
class RequestConsoleTests {
	@Test
	void outputsStrictJsonForSuccessRejectionsAndFailuresWithoutCredentials() throws Exception {
		withConsole(output -> {
			for (int status : new int[]{200, 401, 403, 409, 500, 503}) {
				output.reset();
				var request = new MockHttpServletRequest("POST", "/api/v1/auth/reissue");
				request.addHeader("X-Request-ID", "console-test-" + status);
				request.addHeader("Authorization", "test-sensitive-sentinel");
				request.addHeader("Idempotency-Key", "test-sensitive-sentinel");
				request.setQueryString("test-sensitive-sentinel");
				request.setContent("test-sensitive-sentinel".getBytes(StandardCharsets.UTF_8));
				var response = new MockHttpServletResponse();
				MDC.put("requestId", "outer-request");
				new RequestLoggingFilter().doFilter(request, response, (req, res) -> {
					response.setStatus(status);
					if (status == 503) {
						var error = new QueryTimeoutException("test-sensitive-sentinel",
								new IllegalStateException("test-sensitive-sentinel"));
						error.addSuppressed(new RuntimeException("test-sensitive-sentinel"));
						var auth = new AuthException(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE,
								FailureDiagnostic.from(error, FailureDiagnostic.Operation.SESSION_TRANSACTION));
						var result = new GlobalExceptionHandler().handleBusinessException(auth, request);
						assertThat(result.getStatusCode().value()).isEqualTo(503);
						assertThat(new ObjectMapper().writeValueAsString(result.getBody()))
								.doesNotContain("test-sensitive-sentinel", "DB_TIMEOUT", "diagnostic");
					}
				});
				assertThat(MDC.get("requestId")).isEqualTo("outer-request");
				String console = output.toString(StandardCharsets.UTF_8);
				assertThat(console.lines().toList()).hasSize(1);
				assertThat(console).doesNotContain("test-sensitive-sentinel");
				var json = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(console);
				assertThat(json.path("requestId").asText()).isEqualTo(response.getHeader("X-Request-ID"));
				assertThat(json.path("status").asInt()).isEqualTo(status);
				assertThat(json.path("log.level").asText()).isEqualTo(status >= 500 ? "ERROR" : "INFO");
				if (status == 503) {
					assertThat(json.path("failureKind").asText()).isEqualTo("DB_TIMEOUT");
					assertThat(json.path("operation").asText()).isEqualTo("SESSION_TRANSACTION");
					// A repeated dispatch must not duplicate the ERROR event.
					new RequestLoggingFilter().doFilter(request, response, (req, res) -> { });
					assertThat(output.toString(StandardCharsets.UTF_8).lines().toList()).hasSize(1);
				}
			}
		});
	}

	@Test
	void quietHealthAndJwksAreSilentOnlyOnSuccess() throws Exception {
		withConsole(output -> {
			for (String path : new String[]{"/actuator/health", "/.well-known/jwks.json"}) {
				output.reset();
				new RequestLoggingFilter().doFilter(new MockHttpServletRequest("GET", path),
						new MockHttpServletResponse(), (req, res) -> { });
				assertThat(output.size()).isZero();
				var response = new MockHttpServletResponse();
				new RequestLoggingFilter().doFilter(new MockHttpServletRequest("GET", path), response,
						(req, res) -> response.setStatus(503));
				assertThat(output.toString(StandardCharsets.UTF_8).lines().toList()).hasSize(1);
			}
		});
	}

	private void withConsole(Check check) throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
		Level previousLevel = logger.getLevel();
		boolean previousAdditive = logger.isAdditive();
		var previousMdc = MDC.getCopyOfContextMap();
		PrintStream previousOut = System.out;
		LoggerContext isolated = new LoggerContext();
		isolated.putObject(Environment.class.getName(), new MockEnvironment());
		var encoder = new StructuredLogEncoder();
		encoder.setContext(isolated);
		encoder.setFormat("ecs");
		encoder.start();
		var appender = new ConsoleAppender<ILoggingEvent>();
		appender.setContext(isolated);
		appender.setEncoder(encoder);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(capture);
			appender.start();
			logger.setLevel(Level.INFO);
			logger.setAdditive(false);
			logger.addAppender(appender);
			check.run(output);
			assertThat(isolated.getStatusManager().getCopyOfStatusList())
					.noneMatch(status -> status.getLevel() == Status.ERROR);
		} finally {
			logger.detachAppender(appender);
			appender.stop(); encoder.stop(); isolated.stop();
			logger.setLevel(previousLevel); logger.setAdditive(previousAdditive);
			System.setOut(previousOut);
			if (previousMdc == null) MDC.clear(); else MDC.setContextMap(previousMdc);
		}
	}
	@FunctionalInterface interface Check { void run(ByteArrayOutputStream output) throws Exception; }
}
