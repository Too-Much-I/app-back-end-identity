package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.transaction.support.*;
import org.springframework.dao.DuplicateKeyException;
import com.mongodb.client.result.UpdateResult;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;

/** Mock transaction callback: validates decisions/CAS; replica-set commit/rollback is a staging gate. */
class MongoRecoveryStoreTests {
	static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");
	MongoTemplate mongo = mock(MongoTemplate.class);
	TransactionTemplate tx = mock(TransactionTemplate.class);
	MongoRecoveryStore store = new MongoRecoveryStore(mongo, tx);
	Supplier<RecoveryResult> resolver = mock(Supplier.class);
	@BeforeEach void setup() {
		when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<?>)i.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
		when(resolver.get()).thenReturn(RecoveryResult.notFound());
		when(mongo.updateFirst(any(Query.class), any(Update.class), eq(MongoRecoveryStore.Attempt.class)))
				.thenReturn(UpdateResult.acknowledged(1, 1L, null));
		pending(NOW, NOW.plusSeconds(300));
	}
	void pending(Instant created, Instant expires) {
		when(mongo.findById("attempt", MongoRecoveryStore.Attempt.class))
				.thenReturn(new MongoRecoveryStore.Attempt("attempt", created, expires, null, null, expires.plusSeconds(86400)));
	}
	RecoveryResult lookup(List<String> proofs) { return store.lookup("attempt", proofs, NOW, NOW, Duration.ofMinutes(5), resolver); }
	@Test void claimsAllRetainedProofAliasesAndConsumesByCompareAndSet() {
		assertThat(lookup(List.of("new:hash", "old:hash")).status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
		verify(mongo, times(2)).insert(any(MongoRecoveryStore.Proof.class));
		var query = org.mockito.ArgumentCaptor.forClass(Query.class);
		verify(mongo).updateFirst(query.capture(), any(Update.class), eq(MongoRecoveryStore.Attempt.class));
		assertThat(query.getValue().getQueryObject()).containsEntry("_id", "attempt").containsEntry("proofIds", null);
	}
	@Test void retryIgnoresOriginalChallengeExpiryAndRereadsCurrentOwner() {
		when(mongo.findById("attempt", MongoRecoveryStore.Attempt.class)).thenReturn(new MongoRecoveryStore.Attempt(
				"attempt", NOW.minusSeconds(300), NOW.minusSeconds(1), List.of("old:hash"), NOW.plusSeconds(60), NOW.plusSeconds(86400)));
		when(resolver.get()).thenReturn(RecoveryResult.actionRequired(), RecoveryResult.notFound());
		assertThat(lookup(List.of("new:hash", "old:hash")).status()).isEqualTo(RecoveryResult.Status.ACTION_REQUIRED);
		assertThat(lookup(List.of("old:hash")).status()).isEqualTo(RecoveryResult.Status.NOT_FOUND);
		verify(mongo, never()).insert(any(MongoRecoveryStore.Proof.class));
		assertError(() -> lookup(List.of("different")), AuthErrorStatus.RECOVERY_CONFLICT);
	}
	@Test void rejectsOldSmsExpiredChallengeAndExpiredRetry() {
		pending(NOW.plusSeconds(1), NOW.plusSeconds(300));
		assertError(() -> lookup(List.of("hash")), AuthErrorStatus.RECOVERY_RECENT_AUTH_REQUIRED);
		pending(NOW.minusSeconds(300), NOW);
		assertError(() -> lookup(List.of("hash")), AuthErrorStatus.RECOVERY_EXPIRED);
		when(mongo.findById("attempt", MongoRecoveryStore.Attempt.class)).thenReturn(new MongoRecoveryStore.Attempt(
				"attempt", NOW.minusSeconds(400), NOW.minusSeconds(100), List.of("hash"), NOW, NOW.plusSeconds(86400)));
		assertError(() -> lookup(List.of("hash")), AuthErrorStatus.RECOVERY_EXPIRED);
		verifyNoInteractions(resolver);
	}
	@Test void sameSecondSmsIsAcceptedAtFirebasePrecision() {
		pending(NOW.plusMillis(999), NOW.plusSeconds(300));
		assertThat(lookup(List.of("hash"))).isNotNull();
	}
	@Test void proofClaimCollisionOrFailedCasNeverResolves() {
		doThrow(new DuplicateKeyException("test collision")).when(mongo).insert(any(MongoRecoveryStore.Proof.class));
		assertError(() -> lookup(List.of("hash")), AuthErrorStatus.RECOVERY_CONFLICT);
		reset(mongo); pending(NOW, NOW.plusSeconds(300));
		when(mongo.updateFirst(any(Query.class), any(Update.class), eq(MongoRecoveryStore.Attempt.class)))
				.thenReturn(UpdateResult.acknowledged(0, 0L, null));
		assertError(() -> lookup(List.of("hash")), AuthErrorStatus.RECOVERY_CONFLICT);
		verifyNoInteractions(resolver);
	}
	@Test void budgetDuplicateKeyMeansLimitReached() {
		when(mongo.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(MongoRecoveryStore.Budget.class)))
				.thenThrow(new DuplicateKeyException("test budget"));
		assertError(() -> store.admit(List.of("hmac"), 60, 10, NOW), AuthErrorStatus.RECOVERY_RATE_LIMITED);
	}
	void assertError(Runnable call, AuthErrorStatus status) {
		assertThatThrownBy(call::run).isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(status));
	}
}
