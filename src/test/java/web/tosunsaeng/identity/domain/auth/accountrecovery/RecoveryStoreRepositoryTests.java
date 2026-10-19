package web.tosunsaeng.identity.domain.auth.accountrecovery;

import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.mongodb.client.*;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.transaction.support.*;
import web.tosunsaeng.identity.domain.auth.common.exception.*;

/** In-memory Mongo verifies mapping/index/query behavior, not replica-set transactions. */
class RecoveryStoreRepositoryTests {
	MongoServer server; MongoClient client; MongoTemplate mongo; MongoRecoveryStore first, second;
	Instant now = Instant.parse("2026-10-03T00:00:00Z");
	@BeforeEach void setup() {
		server = new MongoServer(new MemoryBackend()); var address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		mongo = new MongoTemplate(client, "recovery-test");
		for (var type : List.of(MongoRecoveryStore.Attempt.class, MongoRecoveryStore.Proof.class, MongoRecoveryStore.Budget.class)) {
			new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(type)
					.forEach(index -> mongo.indexOps(type).ensureIndex(index));
		}
		TransactionTemplate tx = mock(TransactionTemplate.class);
		when(tx.execute(any())).thenAnswer(i -> ((TransactionCallback<?>)i.getArgument(0)).doInTransaction(new SimpleTransactionStatus()));
		first = new MongoRecoveryStore(mongo, tx); second = new MongoRecoveryStore(mongo, tx);
	}
	@AfterEach void close() { if (client != null) client.close(); if (server != null) server.shutdownNow(); }
	@Test void countersAreSharedAcrossServiceInstancesAndExpireByWindow() {
		for (int i = 0; i < 15; i++) (i % 2 == 0 ? first : second).admit(List.of("hashed-ip"), 60, 15, now);
		assertThatThrownBy(() -> first.admit(List.of("hashed-ip"), 60, 15, now.plusSeconds(40)))
				.isInstanceOfSatisfying(RecoveryRateLimitException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.RECOVERY_RATE_LIMITED);
					assertThat(e.retryAfterSeconds()).isEqualTo(20);
				});
		for (int i = 0; i < 15; i++) second.admit(List.of("hashed-ip"), 60, 15, now.plusSeconds(60));
	}
	@Test void proofUniquenessPreventsUsingAnotherAttemptAndSameAttemptRetries() {
		first.prepare("a", now, now.plusSeconds(300)); second.prepare("b", now, now.plusSeconds(300));
		first.lookup("a", List.of("v1:hashed-proof"), now, now, Duration.ofMinutes(5), RecoveryResult::notFound);
		assertThat(second.lookup("a", List.of("v1:hashed-proof"), now, now, Duration.ofMinutes(5), RecoveryResult::actionRequired))
				.isEqualTo(RecoveryResult.actionRequired());
		assertThatThrownBy(() -> second.lookup("b", List.of("v1:hashed-proof"), now, now, Duration.ofMinutes(5), RecoveryResult::notFound))
				.isInstanceOfSatisfying(AuthException.class, e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorStatus.RECOVERY_CONFLICT));
		assertThat(mongo.findById("b", MongoRecoveryStore.Attempt.class).proofIds()).isNull();
	}
	@Test void ttlIndexesExistAndRawRecordsContainNoCredential() {
		for (var type : List.of(MongoRecoveryStore.Attempt.class, MongoRecoveryStore.Proof.class, MongoRecoveryStore.Budget.class)) {
			assertThat(mongo.indexOps(type).getIndexInfo()).anySatisfy(index -> assertThat(index.getExpireAfter()).contains(Duration.ZERO));
		}
		first.prepare("a", now, now.plusSeconds(300));
		var raw = mongo.getCollection("account_recovery_attempts").find().first();
		assertThat(raw).doesNotContainKeys("phone", "email", "firebaseIdToken", "accessToken", "refreshToken");
	}
}
