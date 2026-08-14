package web.tosunsaeng.identity.global.observability;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-ID";
	public static final String REQUEST_ID_MDC_KEY = "requestId";

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private static final String UNRESOLVED_ROUTE = "UNRESOLVED";
	private static final Set<String> KNOWN_STATIC_ROUTES = Set.of(
			"/.well-known/jwks.json",
			"/actuator/health",
			"/api/v1/auth/check-email",
			"/api/v1/auth/firebase/exchange",
			"/api/v1/auth/firebase/signup",
			"/api/v1/auth/guest",
			"/api/v1/auth/login",
			"/api/v1/auth/logout",
			"/api/v1/auth/logout-all",
			"/api/v1/auth/reissue",
			"/api/v1/auth/signup",
			"/api/v1/users/me",
			"/api/v1/users/me/consents",
			"/api/v1/users/withdraw",
			"/swagger-ui.html",
			"/v3/api-docs"
	);
	private static final Set<String> QUIET_SUCCESS_PATHS = Set.of(
			"/.well-known/jwks.json",
			"/actuator/health",
			"/swagger-ui.html",
			"/v3/api-docs"
	);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String requestId = resolveRequestId(request);
		String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
		long startedAt = System.nanoTime();
		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		try {
			filterChain.doFilter(request, response);
		} catch (IOException | ServletException | RuntimeException | Error exception) {
			RequestLogContext.recordUnexpectedFailure(request, exception);
			throw exception;
		} finally {
			try {
				emitRequestEvent(request, response, requestId, startedAt);
			} finally {
				restoreRequestId(previousRequestId);
			}
		}
	}

	@Override
	protected boolean shouldNotFilterAsyncDispatch() {
		return true;
	}

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return true;
	}

	private void emitRequestEvent(
			HttpServletRequest request,
			HttpServletResponse response,
			String requestId,
			long startedAt
	) {
		RequestLogContext.SafeFailure failure = RequestLogContext.getUnexpectedFailure(request);
		int status = failure == null ? response.getStatus() : Math.max(500, response.getStatus());
		long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

		if (failure != null || status >= 500) {
			emitUnexpectedFailure(request, requestId, status, durationMs, failure);
			return;
		}
		if (status < 400 && isQuietSuccessPath(request.getRequestURI())) {
			return;
		}

		LoggingEventBuilder event = log.atInfo()
				.addKeyValue("event", "http.request.completed")
				.addKeyValue("outcome", status < 400 ? "success" : "rejected")
				.addKeyValue("requestId", requestId)
				.addKeyValue("method", request.getMethod())
				.addKeyValue("route", resolveRoute(request))
				.addKeyValue("status", status)
				.addKeyValue("durationMs", durationMs);
		addErrorCode(event, request);
		event.log("HTTP 요청 처리가 완료되었습니다");
	}

	private void emitUnexpectedFailure(
			HttpServletRequest request,
			String requestId,
			int status,
			long durationMs,
			RequestLogContext.SafeFailure failure
	) {
		if (!RequestLogContext.markErrorLogged(request)) {
			return;
		}

		LoggingEventBuilder event = log.atError()
				.addKeyValue("event", "http.request.failed")
				.addKeyValue("outcome", "unexpected_failure")
				.addKeyValue("requestId", requestId)
				.addKeyValue("method", request.getMethod())
				.addKeyValue("route", resolveRoute(request))
				.addKeyValue("status", status)
				.addKeyValue("durationMs", durationMs);
		addErrorCode(event, request);
		if (failure != null) {
			event.addKeyValue("exceptionType", failure.exceptionType())
					.addKeyValue("causeTypes", failure.causeTypes())
					.addKeyValue("stackTrace", failure.stackTrace());
		}
		event.log("예상하지 못한 오류로 HTTP 요청 처리에 실패했습니다");
	}

	private void addErrorCode(LoggingEventBuilder event, HttpServletRequest request) {
		String errorCode = RequestLogContext.getErrorCode(request);
		if (errorCode != null) {
			event.addKeyValue("errorCode", errorCode);
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		String candidate = request.getHeader(REQUEST_ID_HEADER);
		if (candidate != null
				&& candidate.length() <= 64
				&& SAFE_REQUEST_ID.matcher(candidate).matches()) {
			return candidate;
		}
		return UUID.randomUUID().toString();
	}

	private String resolveRoute(HttpServletRequest request) {
		Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (route != null) {
			return route.toString();
		}
		String requestUri = request.getRequestURI();
		return KNOWN_STATIC_ROUTES.contains(requestUri) ? requestUri : UNRESOLVED_ROUTE;
	}

	private boolean isQuietSuccessPath(String requestUri) {
		return QUIET_SUCCESS_PATHS.contains(requestUri)
				|| requestUri.startsWith("/swagger-ui/")
				|| requestUri.startsWith("/v3/api-docs/");
	}

	private void restoreRequestId(String previousRequestId) {
		if (previousRequestId == null) {
			MDC.remove(REQUEST_ID_MDC_KEY);
			return;
		}
		MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
	}
}
