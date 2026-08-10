package web.tosunsaeng.identity.global.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import jakarta.servlet.ServletException;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import web.tosunsaeng.identity.domain.auth.exception.AuthErrorStatus;
import web.tosunsaeng.identity.domain.auth.exception.AuthException;
import web.tosunsaeng.identity.global.exception.GlobalExceptionHandler;
import web.tosunsaeng.identity.global.response.BaseResponse;
import web.tosunsaeng.identity.support.LogCapture;

class RequestLoggingFilterTests {

	private static final String SAFE_REQUEST_ID = "client-request_123";
	private static final String TEST_ONLY_SENSITIVE_MESSAGE =
			"test-only-sensitive-exception-message";

	private final RequestLoggingFilter filter = new RequestLoggingFilter();
	private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void logsSuccessfulRequestWithValidatedRequestIdAndClearsMdc() throws Exception {
		MockHttpServletRequest request = request("GET", "/api/v1/users/me");
		request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, SAFE_REQUEST_ID);
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
				servletRequest.setAttribute(
						HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
						"/api/v1/users/me"
				);
				assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY))
						.isEqualTo(SAFE_REQUEST_ID);
			});

			assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER))
					.isEqualTo(SAFE_REQUEST_ID);
			assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
			assertThat(logs.events("http.request.completed")).singleElement()
					.satisfies(event -> {
						assertThat(event.getLevel()).isEqualTo(Level.INFO);
						assertThat(event.getFormattedMessage())
								.isEqualTo("HTTP 요청 처리가 완료되었습니다");
						assertThat(LogCapture.value(event, "outcome")).isEqualTo("success");
						assertThat(LogCapture.value(event, "requestId"))
								.isEqualTo(SAFE_REQUEST_ID);
						assertThat(LogCapture.value(event, "route"))
								.isEqualTo("/api/v1/users/me");
						assertThat(LogCapture.value(event, "status")).isEqualTo(200);
					});
		}
	}

	@Test
	void replacesInvalidIncomingRequestIdWithServerUuid() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
		request.addHeader(RequestLoggingFilter.REQUEST_ID_HEADER, "invalid request id");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
				servletRequest.setAttribute(
						HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
						"/api/v1/auth/login"
				);
			});

			String generated = response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
			assertThat(UUID.fromString(generated).toString()).isEqualTo(generated);
			assertThat(logs.events()).singleElement().satisfies(event ->
					assertThat(LogCapture.value(event, "requestId")).isEqualTo(generated)
			);
		}
	}

	@Test
	void unexpectedFailureProducesOneErrorWithoutInfoOrRawMessage() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/v1/auth/reissue");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
				servletRequest.setAttribute(
						HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
						"/api/v1/auth/reissue"
				);
				ResponseEntity<BaseResponse<Void>> handled = exceptionHandler
						.handleUnexpectedException(
								new IllegalStateException(TEST_ONLY_SENSITIVE_MESSAGE),
								(MockHttpServletRequest) servletRequest
						);
				((MockHttpServletResponse) servletResponse)
						.setStatus(handled.getStatusCode().value());
			});
			// 같은 request의 후속 error dispatch가 있어도 ERROR는 다시 생성하지 않는다.
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
			});

			assertThat(logs.events()).singleElement().satisfies(event -> {
				assertThat(event.getLevel()).isEqualTo(Level.ERROR);
				assertThat(event.getFormattedMessage())
						.isEqualTo("예상하지 못한 오류로 HTTP 요청 처리에 실패했습니다");
				assertThat(LogCapture.value(event, "event")).isEqualTo("http.request.failed");
				assertThat(LogCapture.value(event, "errorCode"))
						.isEqualTo("INTERNAL_SERVER_ERROR");
				assertThat(LogCapture.value(event, "exceptionType"))
						.isEqualTo(IllegalStateException.class.getName());
				assertThat(event.getThrowableProxy()).isNull();
				assertThat(LogCapture.rendered(event))
						.doesNotContain(TEST_ONLY_SENSITIVE_MESSAGE);
			});
			assertThat(logs.events("http.request.completed")).isEmpty();
		}
	}

	@Test
	void expectedBusinessFailureProducesInfoWithErrorCodeAndNoError() throws Exception {
		MockHttpServletRequest request = request("POST", "/api/v1/auth/login");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
				servletRequest.setAttribute(
						HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
						"/api/v1/auth/login"
				);
				ResponseEntity<BaseResponse<Void>> handled = exceptionHandler
						.handleBusinessException(
								new AuthException(AuthErrorStatus.INVALID_CREDENTIALS),
								(MockHttpServletRequest) servletRequest
						);
				((MockHttpServletResponse) servletResponse)
						.setStatus(handled.getStatusCode().value());
			});

			assertThat(logs.events()).singleElement().satisfies(event -> {
				assertThat(event.getLevel()).isEqualTo(Level.INFO);
				assertThat(LogCapture.value(event, "event"))
						.isEqualTo("http.request.completed");
				assertThat(LogCapture.value(event, "outcome")).isEqualTo("rejected");
				assertThat(LogCapture.value(event, "errorCode"))
						.isEqualTo("INVALID_CREDENTIALS");
			});
		}
	}

	@Test
	void escapedServletFailureIsLoggedSafelyOnceAndRethrown() {
		MockHttpServletRequest request = request("POST", "/api/v1/users/withdraw");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			assertThatThrownBy(() -> filter.doFilter(
					request,
					response,
					(servletRequest, servletResponse) -> {
						throw new ServletException(TEST_ONLY_SENSITIVE_MESSAGE);
					}
			)).isInstanceOf(ServletException.class);

			assertThat(logs.events()).singleElement().satisfies(event -> {
				assertThat(event.getLevel()).isEqualTo(Level.ERROR);
				assertThat(LogCapture.value(event, "exceptionType"))
						.isEqualTo(ServletException.class.getName());
				assertThat(LogCapture.rendered(event))
						.doesNotContain(TEST_ONLY_SENSITIVE_MESSAGE);
			});
		}
	}

	@Test
	void quietOperationalEndpointSkipsSuccessfulAccessLog() throws Exception {
		MockHttpServletRequest request = request("GET", "/actuator/health");
		MockHttpServletResponse response = new MockHttpServletResponse();

		try (LogCapture logs = LogCapture.forClass(RequestLoggingFilter.class)) {
			filter.doFilter(request, response, (servletRequest, servletResponse) -> {
			});

			assertThat(logs.events()).isEmpty();
			assertThat(response.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER)).isNotBlank();
		}
	}

	private MockHttpServletRequest request(String method, String uri) {
		return new MockHttpServletRequest(method, uri);
	}
}
