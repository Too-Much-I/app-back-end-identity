package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingOutbox;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneFingerprintAliasStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneIdentityStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.PhoneEligibilityBindingOutboxStatus;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneEligibilityFingerprintCandidate;

class PhoneIdentityRepositoryIntegrationTests {

	private static final String USER_A = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String USER_B = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant CREATED_AT = Instant.parse("2026-08-14T01:02:03Z");
	private static final PhoneFingerprint FINGERPRINT_A = new PhoneFingerprint(
			"v1",
			"A".repeat(43)
	);

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private MongoTemplate mongoTemplate;
	private PhoneIdentityRepository identityRepository;
	private PhoneFingerprintAliasRepository aliasRepository;
	private PhoneEligibilityBindingOutboxRepository outboxRepository;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		mongoTemplate = new MongoTemplate(mongoClient, "phone-identity-test");
		ensureIndexes(PhoneIdentity.class);
		ensureIndexes(PhoneFingerprintAlias.class);
		ensureIndexes(PhoneEligibilityBindingOutbox.class);

		MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
		identityRepository = factory.getRepository(PhoneIdentityRepository.class);
		aliasRepository = factory.getRepository(
				PhoneFingerprintAliasRepository.class,
				RepositoryFragments.just(
						new PhoneFingerprintAliasRepositoryImpl(mongoTemplate)
				)
		);
		outboxRepository = factory.getRepository(
				PhoneEligibilityBindingOutboxRepository.class,
				RepositoryFragments.just(
						new PhoneEligibilityBindingOutboxRepositoryCustomImpl(mongoTemplate)
				)
		);
	}

	@AfterEach
	void tearDown() {
		if (mongoClient != null) {
			mongoClient.close();
		}
		if (mongoServer != null) {
			mongoServer.shutdownNow();
		}
	}

	@Test
	void enforcesOneActiveIdentityPerUserAndPersistsReleasedHistory() {
		PhoneIdentity first = identityRepository.save(identity(USER_A, FINGERPRINT_A));

		assertThat(identityRepository.findByUserIdAndStatus(USER_A, PhoneIdentityStatus.ACTIVE))
				.hasValueSatisfying(found -> assertThat(found.getPhoneIdentityId())
						.isEqualTo(first.getPhoneIdentityId()));
		assertThatThrownBy(() -> identityRepository.save(identity(
				USER_A,
				new PhoneFingerprint("v2", "B".repeat(43))
		))).isInstanceOf(DuplicateKeyException.class);

		first.release(CREATED_AT.plusSeconds(1));
		identityRepository.save(first);
		assertThat(identityRepository.findById(first.getPhoneIdentityId()))
				.hasValueSatisfying(released -> assertThat(released.getStatus())
						.isEqualTo(PhoneIdentityStatus.RELEASED));
	}

	@Test
	void enforcesActiveFingerprintAndIdentityVersionUniqueness() {
		PhoneIdentity identityA = identityRepository.save(identity(USER_A, FINGERPRINT_A));
		PhoneIdentity identityB = identityRepository.save(identity(
				USER_B,
				new PhoneFingerprint("v1", "B".repeat(43))
		));
		PhoneFingerprintAlias aliasA = aliasRepository.save(alias(identityA, FINGERPRINT_A));

		assertThatThrownBy(() -> aliasRepository.save(alias(identityB, FINGERPRINT_A)))
				.isInstanceOf(DuplicateKeyException.class);
		assertThatThrownBy(() -> aliasRepository.save(alias(
				identityA,
				new PhoneFingerprint("v1", "C".repeat(43))
		)))
				.isInstanceOf(DuplicateKeyException.class);

		assertThat(aliasRepository.releaseAllActiveByPhoneIdentityId(
				identityA.getPhoneIdentityId(),
				CREATED_AT.plusSeconds(1)
		)).isEqualTo(1);
		assertThat(aliasRepository.findById(aliasA.getAliasId()))
				.hasValueSatisfying(released -> assertThat(released.getStatus())
						.isEqualTo(PhoneFingerprintAliasStatus.RELEASED));
	}

	@Test
	void findsAllRetainedVersionCandidatesWithoutCrossVersionFalseMatch() {
		PhoneIdentity identity = identityRepository.save(identity(USER_A, FINGERPRINT_A));
		PhoneFingerprint oldFingerprint = new PhoneFingerprint("v0", "O".repeat(43));
		aliasRepository.saveAll(List.of(
				alias(identity, oldFingerprint),
				alias(identity, FINGERPRINT_A)
		));

		assertThat(aliasRepository.findAllActiveByFingerprints(List.of(
				new PhoneFingerprint("v0", "X".repeat(43)),
				FINGERPRINT_A
		)))
				.extracting(PhoneFingerprintAlias::getFingerprintKeyVersion)
				.containsExactly("v1");
		assertThat(aliasRepository.findAllActiveByFingerprints(List.of(
				oldFingerprint,
				FINGERPRINT_A
		))).hasSize(2);
	}

	@Test
	void concurrentFingerprintClaimsHaveExactlyOneWinner() throws Exception {
		PhoneIdentity identityA = identityRepository.save(identity(USER_A, FINGERPRINT_A));
		PhoneIdentity identityB = identityRepository.save(identity(
				USER_B,
				new PhoneFingerprint("v2", "B".repeat(43))
		));
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
			Future<Boolean> first = executor.submit(
					() -> claimWhenReleased(identityA, ready, start)
			);
			Future<Boolean> second = executor.submit(
					() -> claimWhenReleased(identityB, ready, start)
			);
			ready.await();
			start.countDown();

			assertThat(List.of(result(first), result(second)))
					.containsExactlyInAnyOrder(true, false);
		}
		assertThat(aliasRepository.findAllActiveByFingerprints(List.of(FINGERPRINT_A)))
				.hasSize(1);
	}

	@Test
	void createsExactPartialUniqueAndLookupIndexes() {
		List<IndexInfo> identityIndexes = mongoTemplate.indexOps(PhoneIdentity.class)
				.getIndexInfo();
		IndexInfo activeUser = index(identityIndexes, "uk_phone_identities_active_user");
		assertThat(activeUser.isUnique()).isTrue();
		assertThat(activeUser.getPartialFilterExpression()).contains("ACTIVE");
		assertThat(activeUser.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("userId");

		List<IndexInfo> aliasIndexes = mongoTemplate.indexOps(PhoneFingerprintAlias.class)
				.getIndexInfo();
		IndexInfo fingerprint = index(aliasIndexes, "uk_phone_aliases_active_fingerprint");
		IndexInfo identityVersion = index(
				aliasIndexes,
				"uk_phone_aliases_active_identity_version"
		);
		IndexInfo userStatus = index(aliasIndexes, "ix_phone_aliases_user_status");
		assertThat(fingerprint.isUnique()).isTrue();
		assertThat(fingerprint.getPartialFilterExpression()).contains("ACTIVE");
		assertThat(fingerprint.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("fingerprintKeyVersion", "phoneFingerprint");
		assertThat(identityVersion.isUnique()).isTrue();
		assertThat(identityVersion.getPartialFilterExpression()).contains("ACTIVE");
		assertThat(identityVersion.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("phoneIdentityId", "fingerprintKeyVersion");
		assertThat(userStatus.isUnique()).isFalse();
		assertThat(userStatus.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("userId", "status");
	}

	@Test
	void eligibilityOutboxStoresRepeatableEventsAndProvidesPublisherIndexes() {
		PhoneEligibilityBindingOutbox first = outbox(USER_A, "consumer-a");
		outboxRepository.save(first);

		assertThat(outboxRepository.findById(first.getEventId()))
				.hasValueSatisfying(stored -> {
					assertThat(stored.getUserId()).isEqualTo(USER_A);
					assertThat(stored.getFingerprintCandidates()).hasSize(1);
					assertThat(stored.toString()).doesNotContain("E".repeat(43));
				});
		outboxRepository.save(outbox(USER_A, "consumer-a"));
		outboxRepository.save(outbox(USER_A, "consumer-b"));
		assertThat(outboxRepository.count()).isEqualTo(3);

		List<IndexInfo> indexes = mongoTemplate
				.indexOps(PhoneEligibilityBindingOutbox.class)
				.getIndexInfo();
		IndexInfo bindingLookup = index(
				indexes,
				"uk_phone_eligibility_outbox_user_scope_revision"
		);
		IndexInfo pendingLookup = index(
				indexes,
				"ix_phone_eligibility_outbox_due"
		);
		assertThat(bindingLookup.isUnique()).isTrue();
		assertThat(bindingLookup.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("userId", "consumerScopeId", "bindingRevision");
		assertThat(pendingLookup.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("consumerScopeId", "status", "nextAttemptAt", "occurredAt");
	}

	@Test
	void outboxClaimIsAtomicAndExpiredLeaseCanBeReclaimed() {
		PhoneEligibilityBindingOutbox event = outbox(USER_A, "consumer-a");
		outboxRepository.save(event);

		PhoneEligibilityBindingOutbox firstClaim = outboxRepository.claimNext(
				"consumer-a", "worker-a", CREATED_AT, CREATED_AT.plusSeconds(60))
				.orElseThrow();
		assertThat(firstClaim.getStatus()).isEqualTo(PhoneEligibilityBindingOutboxStatus.IN_FLIGHT);
		assertThat(firstClaim.getAttemptCount()).isEqualTo(1);
		assertThat(outboxRepository.claimNext(
				"consumer-a", "worker-b", CREATED_AT.plusSeconds(30), CREATED_AT.plusSeconds(90)))
				.isEmpty();

		PhoneEligibilityBindingOutbox reclaimed = outboxRepository.claimNext(
				"consumer-a", "worker-b", CREATED_AT.plusSeconds(60), CREATED_AT.plusSeconds(120))
				.orElseThrow();
		assertThat(reclaimed.getEventId()).isEqualTo(event.getEventId());
		assertThat(reclaimed.getAttemptCount()).isEqualTo(2);
	}

	private boolean claimWhenReleased(
			PhoneIdentity identity,
			CountDownLatch ready,
			CountDownLatch start
	) throws InterruptedException {
		ready.countDown();
		start.await();
		try {
			aliasRepository.save(alias(identity, FINGERPRINT_A));
			return true;
		} catch (DuplicateKeyException exception) {
			return false;
		}
	}

	private boolean result(Future<Boolean> future) throws Exception {
		try {
			return future.get();
		} catch (ExecutionException exception) {
			throw new AssertionError(exception.getCause());
		}
	}

	private void ensureIndexes(Class<?> entityType) {
		MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(
				mongoTemplate.getConverter().getMappingContext()
		);
		resolver.resolveIndexFor(entityType).forEach(indexDefinition -> mongoTemplate
				.indexOps(entityType)
				.ensureIndex(indexDefinition));
	}

	private IndexInfo index(List<IndexInfo> indexes, String name) {
		return indexes.stream()
				.filter(index -> name.equals(index.getName()))
				.findFirst()
				.orElseThrow();
	}

	private PhoneIdentity identity(String userId, PhoneFingerprint fingerprint) {
		return PhoneIdentity.create(userId, fingerprint, CREATED_AT);
	}

	private PhoneFingerprintAlias alias(
			PhoneIdentity identity,
			PhoneFingerprint fingerprint
	) {
		return PhoneFingerprintAlias.create(
				identity.getPhoneIdentityId(),
				identity.getUserId(),
				fingerprint,
				CREATED_AT
		);
	}

	private PhoneEligibilityBindingOutbox outbox(String userId, String scope) {
		return PhoneEligibilityBindingOutbox.createVerified(
				userId,
				scope,
				outboxRepository.count() + 1,
				List.of(new PhoneEligibilityFingerprintCandidate("v1", "E".repeat(43))),
				CREATED_AT,
				CREATED_AT
		);
	}
}
