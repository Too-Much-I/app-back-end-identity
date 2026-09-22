package web.tosunsaeng.identity.domain.auth.session.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static web.tosunsaeng.identity.global.observability.FailureDiagnostic.Kind.*;
import static web.tosunsaeng.identity.global.observability.FailureDiagnostic.Operation.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.dao.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import web.tosunsaeng.identity.domain.auth.common.exception.*;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseIdentityRepository;
import web.tosunsaeng.identity.domain.user.domain.repository.UserRepository;
import web.tosunsaeng.identity.domain.auth.session.domain.UserSessionControl;
import web.tosunsaeng.identity.global.observability.FailureDiagnostic;

class SessionSecurityDiagnosticTests {
	private final MongoTemplate mongo = mock(MongoTemplate.class);
	private final TransactionTemplate tx = mock(TransactionTemplate.class);
	private final SessionSecurityService service = new SessionSecurityService(mongo, tx,
			mock(UserRepository.class), mock(FirebaseIdentityRepository.class));

	static Stream<Arguments> errors() {
		return Stream.of(
				Arguments.of(new QueryTimeoutException("test-sensitive"), DB_TIMEOUT),
				Arguments.of(new DataAccessResourceFailureException("test-sensitive"), DB_UNAVAILABLE),
				Arguments.of(new DataAccessResourceFailureException("test-sensitive", new com.mongodb.MongoTimeoutException("test-sensitive")), DB_TIMEOUT),
				Arguments.of(new OptimisticLockingFailureException("test-sensitive"), DB_CONFLICT),
				Arguments.of(new TransactionSystemException("test-sensitive"), TRANSACTION_FAILURE),
				Arguments.of(new DataIntegrityViolationException("test-sensitive"), DB_FAILURE),
				Arguments.of(new TransactionSystemException("test-sensitive", new QueryTimeoutException("test-sensitive")), DB_TIMEOUT));
	}
	@ParameterizedTest @MethodSource("errors")
	void conversionKeepsOnlySafeDiagnosticAndSamePublicError(RuntimeException source, FailureDiagnostic.Kind expected) {
		when(tx.execute(any())).thenThrow(source);
		for (boolean keepUnique : new boolean[]{false, true}) {
			AuthException error = catchThrowableOfType(() -> {
				if (keepUnique) service.transactionKeepingUniqueConflicts(() -> null);
				else service.transaction(() -> null);
			}, AuthException.class);
			assertThat(error.getErrorCode()).isEqualTo(AuthErrorStatus.SESSION_SECURITY_UNAVAILABLE);
			assertThat(error.getCause()).isNull();
			assertThat(error.getSuppressed()).isEmpty();
			assertThat(error.diagnostic().kind()).isEqualTo(expected);
			assertThat(error.diagnostic().operation()).isEqualTo(SESSION_TRANSACTION);
			assertThat(error.diagnostic().toString()).doesNotContain("test-sensitive");
		}
	}
	@Test void preservesUniqueConflictsAndBusinessErrors() {
		var duplicate = new DuplicateKeyException("test-sensitive");
		when(tx.execute(any())).thenThrow(duplicate);
		assertThatThrownBy(() -> service.transactionKeepingUniqueConflicts(() -> null)).isSameAs(duplicate);
		var business = new AuthException(AuthErrorStatus.INVALID_CREDENTIALS);
		doThrow(business).when(tx).execute(any());
		assertThatThrownBy(() -> service.transaction(() -> null)).isSameAs(business);
	}
	@Test void epochReadHasItsOwnOperation() {
		when(mongo.findById("test-user", UserSessionControl.class)).thenThrow(new QueryTimeoutException("test-sensitive"));
		AuthException error = catchThrowableOfType(() -> service.captureEpoch("test-user"), AuthException.class);
		assertThat(error.diagnostic().operation()).isEqualTo(SESSION_EPOCH_READ);
		assertThat(error.diagnostic().kind()).isEqualTo(DB_TIMEOUT);
	}
	@Test void boundsCauseCyclesAndNeverCopiesMessagesOrSuppressedExceptions() {
		var first = new RuntimeException("test-sensitive");
		var second = new RuntimeException("test-sensitive", first);
		first.initCause(second);
		first.addSuppressed(new QueryTimeoutException("test-sensitive"));
		var diagnostic = FailureDiagnostic.from(first, SESSION_TRANSACTION);
		assertThat(diagnostic.kind()).isEqualTo(UNKNOWN);
		assertThat(diagnostic.toString()).doesNotContain("test-sensitive", "QueryTimeoutException");
		assertThat(diagnostic.causeTypes().length()).isLessThan(1500);
		var mongoConflict = new com.mongodb.MongoException(112, "test-sensitive");
		assertThat(FailureDiagnostic.from(mongoConflict, SESSION_TRANSACTION).kind()).isEqualTo(DB_CONFLICT);
		var commitUnknown = new com.mongodb.MongoException("test-sensitive");
		commitUnknown.addLabel(com.mongodb.MongoException.UNKNOWN_TRANSACTION_COMMIT_RESULT_LABEL);
		assertThat(FailureDiagnostic.from(commitUnknown, SESSION_TRANSACTION).kind()).isEqualTo(TRANSACTION_FAILURE);
	}
}
