package web.tosunsaeng.identity.global.observability;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.regex.Pattern;

import com.mongodb.MongoException;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.TransactionException;
import web.tosunsaeng.identity.domain.auth.common.exception.AuthException;

/** Bounded metadata only: no Throwable reference, message, query, identifiers or credentials. */
public record FailureDiagnostic(Operation operation, Kind kind, String exceptionType,
                                String causeTypes, String stackTrace) {

	public enum Operation { SESSION_TRANSACTION, SESSION_EPOCH_READ, SESSION_REVOCATION_BATCH, PROVIDER_CHANGE_BATCH }
	public enum Kind { DB_TIMEOUT, DB_UNAVAILABLE, DB_CONFLICT, TRANSACTION_FAILURE, DB_FAILURE, UNKNOWN }
	private static final Pattern CODE = Pattern.compile("[A-Za-z0-9_.$<>-]{1,240}");

	public static FailureDiagnostic from(Throwable exception, Operation operation) {
		if (exception instanceof AuthException auth && auth.diagnostic() != null) return auth.diagnostic();
		Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		StringBuilder causes = new StringBuilder();
		Kind kind = Kind.UNKNOWN;
		Throwable current = exception;
		for (int depth = 0; current != null && depth < 6 && seen.add(current); depth++, current = current.getCause()) {
			if (depth > 0) {
				if (!causes.isEmpty()) causes.append(" <- ");
				causes.append(safe(current.getClass().getName()));
			}
			Kind candidate = classify(current);
			// Specific DB causes take precedence over a wrapping transaction/data-access error.
			if (rank(candidate) >= rank(kind)) kind = candidate;
		}
		StringBuilder frames = new StringBuilder();
		StackTraceElement[] stack = exception.getStackTrace();
		for (int i = 0; i < Math.min(stack.length, 24); i++) {
			if (!frames.isEmpty()) frames.append(" | ");
			frames.append(safe(stack[i].getClassName())).append('.').append(safe(stack[i].getMethodName()))
					.append(':').append(stack[i].getLineNumber());
		}
		return new FailureDiagnostic(operation, kind, safe(exception.getClass().getName()),
				causes.isEmpty() ? "none" : causes.toString(), frames.isEmpty() ? "unavailable" : frames.toString());
	}

	private static Kind classify(Throwable error) {
		if (error instanceof QueryTimeoutException || error instanceof MongoTimeoutException
				|| error instanceof com.mongodb.MongoExecutionTimeoutException) return Kind.DB_TIMEOUT;
		if (error instanceof DataAccessResourceFailureException || error instanceof MongoSocketException) return Kind.DB_UNAVAILABLE;
		if (error instanceof ConcurrencyFailureException || error instanceof DuplicateKeyException) return Kind.DB_CONFLICT;
		if (error instanceof MongoException mongo) {
			if (mongo.getCode() == 112) return Kind.DB_CONFLICT; // Mongo WriteConflict
			if (mongo.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
					|| mongo.hasErrorLabel(MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL)) return Kind.TRANSACTION_FAILURE;
			return Kind.DB_FAILURE;
		}
		if (error instanceof TransactionException) return Kind.TRANSACTION_FAILURE;
		if (error instanceof DataAccessException) return Kind.DB_FAILURE;
		return Kind.UNKNOWN;
	}

	private static int rank(Kind kind) {
		return switch (kind) {
			case UNKNOWN -> 0;
			case DB_FAILURE -> 1;
			case TRANSACTION_FAILURE -> 2;
			case DB_TIMEOUT, DB_UNAVAILABLE, DB_CONFLICT -> 3;
		};
	}

	private static String safe(String value) {
		return value != null && CODE.matcher(value).matches() ? value : "unknown";
	}

	public LoggingEventBuilder attachTo(LoggingEventBuilder event) {
		return event.addKeyValue("operation", operation.name()).addKeyValue("failureKind", kind.name())
				.addKeyValue("exceptionType", exceptionType).addKeyValue("causeTypes", causeTypes)
				.addKeyValue("stackTrace", stackTrace);
	}
}
