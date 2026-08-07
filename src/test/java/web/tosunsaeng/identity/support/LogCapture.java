package web.tosunsaeng.identity.support;

import java.util.List;
import java.util.Objects;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

public final class LogCapture implements AutoCloseable {

	private final Logger logger;
	private final Level previousLevel;
	private final boolean previousAdditive;
	private final ListAppender<ILoggingEvent> appender;

	private LogCapture(Class<?> loggerType) {
		logger = (Logger) LoggerFactory.getLogger(loggerType);
		previousLevel = logger.getLevel();
		previousAdditive = logger.isAdditive();
		appender = new ListAppender<>();
		appender.start();
		logger.setLevel(Level.TRACE);
		logger.setAdditive(false);
		logger.addAppender(appender);
	}

	public static LogCapture forClass(Class<?> loggerType) {
		return new LogCapture(Objects.requireNonNull(loggerType, "loggerType must not be null"));
	}

	public List<ILoggingEvent> events() {
		return List.copyOf(appender.list);
	}

	public List<ILoggingEvent> events(String eventName) {
		return appender.list.stream()
				.filter(event -> eventName.equals(value(event, "event")))
				.toList();
	}

	public static Object value(ILoggingEvent event, String key) {
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (keyValuePairs == null) {
			return null;
		}
		return keyValuePairs.stream()
				.filter(pair -> key.equals(pair.key))
				.map(pair -> pair.value)
				.findFirst()
				.orElse(null);
	}

	public static String rendered(ILoggingEvent event) {
		StringBuilder rendered = new StringBuilder(event.getFormattedMessage());
		List<KeyValuePair> keyValuePairs = event.getKeyValuePairs();
		if (keyValuePairs != null) {
			keyValuePairs.forEach(pair -> rendered
					.append(" ")
					.append(pair.key)
					.append("=")
					.append(pair.value));
		}
		return rendered.toString();
	}

	@Override
	public void close() {
		logger.detachAppender(appender);
		appender.stop();
		logger.setLevel(previousLevel);
		logger.setAdditive(previousAdditive);
	}
}
