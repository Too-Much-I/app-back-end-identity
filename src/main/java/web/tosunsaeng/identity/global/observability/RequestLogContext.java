package web.tosunsaeng.identity.global.observability;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestLogContext {

	private static final String ATTRIBUTE_PREFIX = RequestLogContext.class.getName() + ".";
	private static final String ERROR_CODE_ATTRIBUTE = ATTRIBUTE_PREFIX + "errorCode";
	private static final String FAILURE_ATTRIBUTE = ATTRIBUTE_PREFIX + "unexpectedFailure";
	private static final String ERROR_LOGGED_ATTRIBUTE = ATTRIBUTE_PREFIX + "errorLogged";
	private static final int MAX_STACK_FRAMES = 24;
	private static final int MAX_CAUSE_TYPES = 5;

	private RequestLogContext() {
	}

	public static void recordErrorCode(HttpServletRequest request, String errorCode) {
		Objects.requireNonNull(request, "request must not be null");
		if (errorCode != null && !errorCode.isBlank()) {
			request.setAttribute(ERROR_CODE_ATTRIBUTE, errorCode);
		}
	}

	public static String getErrorCode(HttpServletRequest request) {
		Object errorCode = request.getAttribute(ERROR_CODE_ATTRIBUTE);
		return errorCode instanceof String value ? value : null;
	}

	public static void recordUnexpectedFailure(
			HttpServletRequest request,
			Throwable exception
	) {
		Objects.requireNonNull(request, "request must not be null");
		Objects.requireNonNull(exception, "exception must not be null");
		if (request.getAttribute(FAILURE_ATTRIBUTE) == null) {
			request.setAttribute(FAILURE_ATTRIBUTE, SafeFailure.from(exception));
		}
	}

	static SafeFailure getUnexpectedFailure(HttpServletRequest request) {
		Object failure = request.getAttribute(FAILURE_ATTRIBUTE);
		return failure instanceof SafeFailure safeFailure ? safeFailure : null;
	}

	static boolean markErrorLogged(HttpServletRequest request) {
		if (Boolean.TRUE.equals(request.getAttribute(ERROR_LOGGED_ATTRIBUTE))) {
			return false;
		}
		request.setAttribute(ERROR_LOGGED_ATTRIBUTE, Boolean.TRUE);
		return true;
	}

	record SafeFailure(
			String exceptionType,
			String causeTypes,
			String stackTrace
	) {

		private static SafeFailure from(Throwable exception) {
			return new SafeFailure(
					exception.getClass().getName(),
					causeTypes(exception),
					stackTrace(exception)
			);
		}

		private static String causeTypes(Throwable exception) {
			StringBuilder causeTypes = new StringBuilder();
			Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
			Throwable cause = exception.getCause();
			int count = 0;
			while (cause != null && count < MAX_CAUSE_TYPES && visited.add(cause)) {
				if (!causeTypes.isEmpty()) {
					causeTypes.append(" <- ");
				}
				causeTypes.append(cause.getClass().getName());
				cause = cause.getCause();
				count++;
			}
			return causeTypes.isEmpty() ? "none" : causeTypes.toString();
		}

		private static String stackTrace(Throwable exception) {
			StackTraceElement[] elements = exception.getStackTrace();
			if (elements.length == 0) {
				return "unavailable";
			}

			StringBuilder stackTrace = new StringBuilder();
			int limit = Math.min(elements.length, MAX_STACK_FRAMES);
			for (int index = 0; index < limit; index++) {
				if (!stackTrace.isEmpty()) {
					stackTrace.append(" | ");
				}
				stackTrace.append(elements[index]);
			}
			if (elements.length > limit) {
				stackTrace.append(" | ... ");
				stackTrace.append(elements.length - limit);
				stackTrace.append(" more");
			}
			return stackTrace.toString();
		}
	}
}
