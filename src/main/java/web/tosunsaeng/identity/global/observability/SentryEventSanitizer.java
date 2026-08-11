package web.tosunsaeng.identity.global.observability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import io.sentry.Hint;
import io.sentry.SentryEvent;
import io.sentry.SentryOptions;
import io.sentry.protocol.DebugImage;
import io.sentry.protocol.DebugMeta;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class SentryEventSanitizer implements SentryOptions.BeforeSendCallback {

	private static final String JAVA_PLATFORM = "java";
	private static final String UNKNOWN_EXCEPTION_TYPE = "UnknownException";
	private static final Set<String> HTTP_METHODS = Set.of(
			"GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"
	);
	private static final Pattern CODE_NAME = Pattern.compile(
			"(?:[A-Za-z_$][A-Za-z0-9_$]*\\.)*[A-Za-z_$][A-Za-z0-9_$]*"
	);
	private static final Pattern FUNCTION_NAME = Pattern.compile("[A-Za-z0-9_$<>]+");
	private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_$.-]+");
	private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private static final Pattern ERROR_CODE = Pattern.compile("[A-Z0-9_]{1,64}");
	private static final Pattern ROUTE = Pattern.compile("/[A-Za-z0-9_{}.*:/-]{0,199}");
	private static final Pattern ENVIRONMENT = Pattern.compile("[A-Za-z0-9._-]{1,64}");
	private static final Pattern RELEASE = Pattern.compile("[A-Za-z0-9._@+/:=-]{1,200}");
	private static final Pattern HTTP_STATUS = Pattern.compile("5[0-9]{2}");
	private static final Pattern DEBUG_ID = Pattern.compile(
			"[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-"
					+ "[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
	);

	private final String trustedEnvironment;
	private final String trustedRelease;

	public SentryEventSanitizer(
			@Value("${sentry.environment:}") String trustedEnvironment,
			@Value("${sentry.release:}") String trustedRelease
	) {
		this.trustedEnvironment = trustedValue(trustedEnvironment, ENVIRONMENT);
		this.trustedRelease = trustedValue(trustedRelease, RELEASE);
	}

	@Override
	public SentryEvent execute(SentryEvent event, Hint hint) {
		if (event == null) {
			return null;
		}

		// SDK와 integration이 붙인 필드를 부분 삭제하지 않고 허용 정보만 새 event로 옮긴다.
		SentryEvent sanitized = new SentryEvent(event.getTimestamp());
		sanitized.setEventId(event.getEventId());
		sanitized.setLevel(event.getLevel());
		sanitized.setPlatform(JAVA_PLATFORM);
		sanitized.setEnvironment(trustedEnvironment);
		sanitized.setRelease(trustedRelease);
		sanitized.setTags(sanitizeTags(event.getTags()));
		sanitized.setExceptions(sanitizeExceptions(event.getExceptions()));
		sanitized.setDebugMeta(sanitizeDebugMeta(event.getDebugMeta()));
		return sanitized;
	}

	private DebugMeta sanitizeDebugMeta(DebugMeta debugMeta) {
		if (debugMeta == null || debugMeta.getImages() == null) {
			return null;
		}

		List<DebugImage> images = new ArrayList<>();
		for (DebugImage image : debugMeta.getImages()) {
			if (image == null
					|| !DebugImage.JVM.equals(image.getType())
					|| !matches(image.getDebugId(), DEBUG_ID)) {
				continue;
			}
			DebugImage safeImage = new DebugImage();
			safeImage.setType(DebugImage.JVM);
			safeImage.setDebugId(image.getDebugId());
			images.add(safeImage);
		}

		if (images.isEmpty()) {
			return null;
		}
		DebugMeta sanitized = new DebugMeta();
		sanitized.setImages(images);
		return sanitized;
	}

	private Map<String, String> sanitizeTags(Map<String, String> tags) {
		if (tags == null || tags.isEmpty()) {
			return null;
		}

		Map<String, String> sanitized = new LinkedHashMap<>();
		putIfValid(sanitized, "requestId", tags.get("requestId"), REQUEST_ID);
		putIfValid(sanitized, "errorCode", tags.get("errorCode"), ERROR_CODE);

		String method = tags.get("http.method");
		if (method != null && HTTP_METHODS.contains(method)) {
			sanitized.put("http.method", method);
		}

		String route = tags.get("http.route");
		if ("UNRESOLVED".equals(route) || matches(route, ROUTE)) {
			sanitized.put("http.route", route);
		}
		putIfValid(sanitized, "http.status_code", tags.get("http.status_code"), HTTP_STATUS);
		return sanitized.isEmpty() ? null : sanitized;
	}

	private List<SentryException> sanitizeExceptions(List<SentryException> exceptions) {
		if (exceptions == null || exceptions.isEmpty()) {
			return null;
		}

		List<SentryException> sanitized = new ArrayList<>(exceptions.size());
		for (SentryException exception : exceptions) {
			if (exception == null) {
				continue;
			}
			SentryException safeException = new SentryException();
			safeException.setType(safeCodeName(exception.getType(), UNKNOWN_EXCEPTION_TYPE));
			safeException.setModule(safeCodeName(exception.getModule(), null));
			safeException.setStacktrace(sanitizeStackTrace(exception.getStacktrace()));
			sanitized.add(safeException);
		}
		return sanitized.isEmpty() ? null : sanitized;
	}

	private SentryStackTrace sanitizeStackTrace(SentryStackTrace stackTrace) {
		if (stackTrace == null || stackTrace.getFrames() == null) {
			return null;
		}

		List<SentryStackFrame> frames = new ArrayList<>(stackTrace.getFrames().size());
		for (SentryStackFrame frame : stackTrace.getFrames()) {
			if (frame == null) {
				continue;
			}
			SentryStackFrame safeFrame = new SentryStackFrame();
			safeFrame.setModule(safeCodeName(frame.getModule(), null));
			safeFrame.setFunction(safeValue(frame.getFunction(), FUNCTION_NAME, 200));
			safeFrame.setFilename(safeValue(frame.getFilename(), FILE_NAME, 200));
			safeFrame.setLineno(frame.getLineno());
			safeFrame.setColno(frame.getColno());
			safeFrame.setInApp(frame.isInApp());
			safeFrame.setPlatform(JAVA_PLATFORM);
			safeFrame.setNative(frame.isNative());
			frames.add(safeFrame);
		}

		if (frames.isEmpty()) {
			return null;
		}
		return new SentryStackTrace(frames);
	}

	private void putIfValid(
			Map<String, String> destination,
			String key,
			String value,
			Pattern pattern
	) {
		if (matches(value, pattern)) {
			destination.put(key, value);
		}
	}

	private String safeCodeName(String value, String fallback) {
		return safeValue(value, CODE_NAME, 240) == null ? fallback : value;
	}

	private String safeValue(String value, Pattern pattern, int maxLength) {
		return value != null && value.length() <= maxLength && pattern.matcher(value).matches()
				? value
				: null;
	}

	private boolean matches(String value, Pattern pattern) {
		return value != null && pattern.matcher(value).matches();
	}

	private static String trustedValue(String value, Pattern pattern) {
		if (value == null) {
			return null;
		}
		String candidate = value.trim();
		return pattern.matcher(candidate).matches() ? candidate : null;
	}
}
