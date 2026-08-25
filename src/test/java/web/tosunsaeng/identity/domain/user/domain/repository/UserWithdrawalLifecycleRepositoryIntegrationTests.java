package web.tosunsaeng.identity.domain.user.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import web.tosunsaeng.identity.domain.user.domain.entity.UserWithdrawalLifecycle;
import web.tosunsaeng.identity.domain.user.domain.enums.UserWithdrawalCleanupStatus;
import web.tosunsaeng.identity.domain.user.domain.enums.WithdrawalCleanupFailureCode;

class UserWithdrawalLifecycleRepositoryIntegrationTests {

	private static final String USER_A = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String USER_B = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant NOW = Instant.parse("2026-08-25T03:00:00Z");

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private MongoTemplate mongoTemplate;
	private UserWithdrawalLifecycleRepository repository;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		mongoTemplate = new MongoTemplate(mongoClient, "withdrawal-cleanup-test");
		MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(
				mongoTemplate.getConverter().getMappingContext()
		);
		resolver.resolveIndexFor(UserWithdrawalLifecycle.class).forEach(index -> mongoTemplate
				.indexOps(UserWithdrawalLifecycle.class)
				.ensureIndex(index));
		MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
		repository = factory.getRepository(
				UserWithdrawalLifecycleRepository.class,
				RepositoryFragments.just(
						new UserWithdrawalLifecycleRepositoryImpl(mongoTemplate)
				)
		);
	}

	@AfterEach
	void tearDown() {
		if (mongoClient != null) mongoClient.close();
		if (mongoServer != null) mongoServer.shutdownNow();
	}

	@Test
	void claimIsAtomicAndUsesUniqueTokenAndVersionFencing() {
		UserWithdrawalLifecycle saved = repository.save(firebaseLifecycle(USER_A));

		UserWithdrawalLifecycle first = repository.claimNext(
				"lease-1", NOW, NOW.plusSeconds(30)
		).orElseThrow();
		assertThat(first.getWithdrawalId()).isEqualTo(saved.getWithdrawalId());
		assertThat(first.getStatus())
				.isEqualTo(UserWithdrawalCleanupStatus.EXTERNAL_CLEANUP_IN_PROGRESS);
		assertThat(first.getLeaseOwner()).isEqualTo("lease-1");
		assertThat(first.getAttemptCount()).isEqualTo(1);
		assertThat(first.getVersion()).isEqualTo(saved.getVersion() + 1);
		assertThat(repository.claimNext("lease-2", NOW.plusSeconds(1), NOW.plusSeconds(31)))
				.isEmpty();

		UserWithdrawalLifecycle recovered = repository.claimNext(
				"lease-2", NOW.plusSeconds(30), NOW.plusSeconds(60)
		).orElseThrow();
		assertThat(recovered.getLeaseOwner()).isEqualTo("lease-2");
		assertThat(recovered.getAttemptCount()).isEqualTo(2);
		assertThat(recovered.getVersion()).isEqualTo(first.getVersion() + 1);
		assertThat(repository.markExternalCleanupCompleted(
				first.getWithdrawalId(), "lease-1", first.getVersion(), NOW.plusSeconds(31)
		)).isFalse();
		assertThat(repository.markExternalCleanupCompleted(
				recovered.getWithdrawalId(), "lease-2", recovered.getVersion(), NOW.plusSeconds(31)
		)).isTrue();
	}

	@Test
	void retryCannotBeClaimedBeforeDueAndMaxAttemptEvidenceIsStored() {
		repository.save(firebaseLifecycle(USER_A));
		UserWithdrawalLifecycle claimed = repository.claimNext(
				"lease-1", NOW, NOW.plusSeconds(30)
		).orElseThrow();
		Instant retryAt = NOW.plusSeconds(20);

		assertThat(repository.scheduleRetry(
				claimed.getWithdrawalId(), "lease-1", claimed.getVersion(),
				WithdrawalCleanupFailureCode.TIMEOUT, retryAt, NOW.plusSeconds(1)
		)).isTrue();
		assertThat(repository.claimNext("early", retryAt.minusMillis(1), retryAt.plusSeconds(30)))
				.isEmpty();
		UserWithdrawalLifecycle retried = repository.claimNext(
				"due", retryAt, retryAt.plusSeconds(30)
		).orElseThrow();
		assertThat(repository.markReconciliationRequired(
				retried.getWithdrawalId(), "due", retried.getVersion(),
				WithdrawalCleanupFailureCode.TIMEOUT, true, retryAt.plusSeconds(1)
		)).isTrue();
		assertThat(repository.findById(retried.getWithdrawalId()))
				.hasValueSatisfying(lifecycle -> {
					assertThat(lifecycle.getStatus())
							.isEqualTo(UserWithdrawalCleanupStatus.RECONCILIATION_REQUIRED);
					assertThat(lifecycle.getLastErrorCode()).isEqualTo("TIMEOUT");
					assertThat(lifecycle.isMaxAttemptsExceeded()).isTrue();
					assertThat(lifecycle.getLeaseOwner()).isNull();
				});
	}

	@Test
	void completedLifecycleIsHandedOffWithoutAnotherExternalClaim() {
		repository.save(firebaseLifecycle(USER_A));
		UserWithdrawalLifecycle claimed = repository.claimNext(
				"lease-1", NOW, NOW.plusSeconds(30)
		).orElseThrow();
		assertThat(repository.markExternalCleanupCompleted(
				claimed.getWithdrawalId(), "lease-1", claimed.getVersion(), NOW.plusSeconds(1)
		)).isTrue();

		assertThat(repository.claimNext("external", NOW.plusSeconds(2), NOW.plusSeconds(32)))
				.isEmpty();
		assertThat(repository.handoffNextCompleted(NOW.plusSeconds(2)))
				.hasValueSatisfying(handoff -> {
					assertThat(handoff.getStatus())
							.isEqualTo(UserWithdrawalCleanupStatus.IDENTITY_RELEASE_PENDING);
					assertThat(handoff.getExternalDeletedAt()).isEqualTo(NOW.plusSeconds(1));
				});
		assertThat(repository.handoffNextCompleted(NOW.plusSeconds(3))).isEmpty();
	}

	@Test
	void claimsOldestDueLifecycleAndDeclaresExactWorkerIndex() {
		repository.save(UserWithdrawalLifecycle.create(USER_A, null, null, NOW.minusSeconds(1)));
		repository.save(firebaseLifecycle(USER_B));

		UserWithdrawalLifecycle claimed = repository.claimNext(
				"lease", NOW, NOW.plusSeconds(30)
		).orElseThrow();
		assertThat(claimed.getUserId()).isEqualTo(USER_A);

		List<IndexInfo> indexes = mongoTemplate.indexOps(UserWithdrawalLifecycle.class)
				.getIndexInfo();
		IndexInfo workerIndex = indexes.stream()
				.filter(index -> "ix_withdrawal_lifecycle_worker_claim".equals(index.getName()))
				.findFirst()
				.orElseThrow();
		assertThat(workerIndex.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("status", "nextAttemptAt", "leaseUntil");
	}

	private UserWithdrawalLifecycle firebaseLifecycle(String userId) {
		return UserWithdrawalLifecycle.create(userId, "test-project", "opaque-uid", NOW);
	}
}
