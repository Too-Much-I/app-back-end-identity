package web.tosunsaeng.identity.global.observability;

import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import io.sentry.Sentry;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerMapping;

public final class SentryExceptionReporter {

	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private static final Pattern SAFE_ERROR_CODE = Pattern.compile("[A-Z0-9_]{1,64}");
	private static final Pattern SAFE_ROUTE = Pattern.compile("/[A-Za-z0-9_{}.*:/-]{0,199}");
	private static final Set<String> HTTP_METHODS = Set.of(
			"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"
	);

	public void captureUnexpected(
			Exception exception,
			HttpServletRequest request,
			String errorCode
	) {
		String requestId = safeValue(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY), SAFE_REQUEST_ID);
		String method = HTTP_METHODS.contains(request.getMethod()) ? request.getMethod() : null;
		String route = resolveRoute(request);
		String safeErrorCode = safeValue(errorCode, SAFE_ERROR_CODE);

		Sentry.captureException(exception, scope -> {
			// 현재 요청 scope의 user, request, breadcrumb, extra가 event로 복사되지 않게 먼저 비운다.
			scope.clear();
			setTag(scope::setTag, "requestId", requestId);
			setTag(scope::setTag, "errorCode", safeErrorCode);
			setTag(scope::setTag, "http.method", method);
			setTag(scope::setTag, "http.route", route);
			scope.setTag("http.status_code", "500");
		});
	}

	private String resolveRoute(HttpServletRequest request) {
		Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
		if (route == null) {
			return "UNRESOLVED";
		}
		return safeValue(route.toString(), SAFE_ROUTE) == null
				? "UNRESOLVED"
				: route.toString();
	}

	private String safeValue(String value, Pattern pattern) {
		return value != null && pattern.matcher(value).matches() ? value : null;
	}

	private void setTag(TagSetter setter, String key, String value) {
		if (value != null) {
			setter.set(key, value);
		}
	}

	@FunctionalInterface
	private interface TagSetter {
		void set(String key, String value);
	}
}
