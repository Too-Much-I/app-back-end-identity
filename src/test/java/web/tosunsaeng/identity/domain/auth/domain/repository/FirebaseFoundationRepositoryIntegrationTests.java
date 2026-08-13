package web.tosunsaeng.identity.domain.auth.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
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

import web.tosunsaeng.identity.domain.auth.application.firebase.FirebaseAuthenticationMethod;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentStatus;

class FirebaseFoundationRepositoryIntegrationTests {

	private static final String USER_A = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String USER_B = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant CREATED_AT = Instant.parse("2026-08-13T01:02:03Z");

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private MongoTemplate mongoTemplate;
	private FirebaseIdentityRepository identityRepository;
	private FirebaseEnrollmentAttemptRepository enrollmentRepository;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		mongoTemplate = new MongoTemplate(mongoClient, "firebase-foundation-test");
		ensureIndexes(FirebaseIdentity.class);
		ensureIndexes(FirebaseEnrollmentAttempt.class);

		MongoRepositoryFactory factory = new MongoRepositoryFactory(mongoTemplate);
		identityRepository = factory.getRepository(FirebaseIdentityRepository.class);
		enrollmentRepository = factory.getRepository(
				FirebaseEnrollmentAttemptRepository.class,
				RepositoryFragments.just(
						new FirebaseEnrollmentAttemptRepositoryImpl(mongoTemplate)
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
	void firebaseIdentityEnforcesProjectUidAndCanonicalUserUniqueness() {
		FirebaseIdentity first = identity("test-project", "Opaque_UID", USER_A);
		identityRepository.save(first);

		assertThat(identityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project",
				"Opaque_UID"
		)).hasValueSatisfying(identity -> assertThat(identity.getUserId()).isEqualTo(USER_A));
		assertThat(identityRepository.findByFirebaseProjectIdAndFirebaseUid(
				"test-project",
				"opaque_uid"
		)).isEmpty();
		assertThat(identityRepository.findByUserId(USER_A))
				.hasValueSatisfying(identity -> {
					assertThat(identity.getFirebaseIdentityId())
							.isEqualTo(first.getFirebaseIdentityId());
					assertThat(identity.getFirebaseProjectId()).isEqualTo("test-project");
					assertThat(identity.getFirebaseUid()).isEqualTo("Opaque_UID");
				});

		assertThatThrownBy(() -> identityRepository.save(
				identity("test-project", "Opaque_UID", USER_B)
		)).isInstanceOf(DuplicateKeyException.class);
		assertThatThrownBy(() -> identityRepository.save(
				identity("test-project", "different-uid", USER_A)
		)).isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void enrollmentPartialUniqueAllowsOnlyOnePendingAttemptPerBinding() {
		FirebaseEnrollmentAttempt first = directAttempt(CREATED_AT);
		enrollmentRepository.save(first);

		Document stored = mongoTemplate.getCollection("firebase_enrollment_attempts")
				.find(new Document("_id", first.getEnrollmentId()))
				.first();
		assertThat(stored).isNotNull();
		assertThat(stored.containsKey("boundUserId")).isTrue();
		assertThat(stored.get("boundUserId")).isNull();

		assertThatThrownBy(() -> enrollmentRepository.save(directAttempt(CREATED_AT.plusSeconds(1))))
				.isInstanceOf(DuplicateKeyException.class);

		Instant consumedAt = CREATED_AT.plusSeconds(30);
		assertThat(enrollmentRepository.consumeIfPendingAndNotExpired(
				first.getEnrollmentId(),
				"test-project",
				"different-uid",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				consumedAt
		)).isFalse();
		assertThat(enrollmentRepository.consumeIfPendingAndNotExpired(
				first.getEnrollmentId(),
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				consumedAt
		)).isTrue();
		assertThat(enrollmentRepository.consumeIfPendingAndNotExpired(
				first.getEnrollmentId(),
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				consumedAt
		)).isFalse();
		assertThat(enrollmentRepository.findById(first.getEnrollmentId()))
				.hasValueSatisfying(consumed -> {
					assertThat(consumed.getStatus()).isEqualTo(FirebaseEnrollmentStatus.CONSUMED);
					assertThat(consumed.getConsumedAt()).isEqualTo(consumedAt);
				});
		assertThat(enrollmentRepository.findPendingByBinding(
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		)).isEmpty();
	}

	@Test
	void expirationAndConsumptionUseApplicationTimeInsteadOfTtlDeletion() {
		FirebaseEnrollmentAttempt attempt = directAttempt(CREATED_AT);
		enrollmentRepository.save(attempt);
		Instant exactExpiry = attempt.getExpiresAt();

		assertThat(enrollmentRepository.consumeIfPendingAndNotExpired(
				attempt.getEnrollmentId(),
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				exactExpiry
		)).isFalse();
		assertThat(enrollmentRepository.expireIfPendingAndExpired(
				attempt.getEnrollmentId(),
				exactExpiry
		)).isTrue();
		assertThat(enrollmentRepository.expireIfPendingAndExpired(
				attempt.getEnrollmentId(),
				exactExpiry
		)).isFalse();
		assertThat(enrollmentRepository.findById(attempt.getEnrollmentId()))
				.hasValueSatisfying(expired -> assertThat(expired.getStatus())
						.isEqualTo(FirebaseEnrollmentStatus.EXPIRED));
	}

	@Test
	void guestBindingsAreSeparatedByCanonicalGuestUserId() {
		enrollmentRepository.save(guestAttempt(USER_A));
		enrollmentRepository.save(guestAttempt(USER_B));

		assertThat(enrollmentRepository.findPendingByBinding(
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.GUEST_USER,
				USER_A
		)).hasValueSatisfying(attempt -> assertThat(attempt.getBoundUserId()).isEqualTo(USER_A));
		assertThat(enrollmentRepository.count()).isEqualTo(2);
	}

	@Test
	void createsExactUniquePartialAndCleanupIndexes() {
		List<IndexInfo> identityIndexes = mongoTemplate.indexOps(FirebaseIdentity.class)
				.getIndexInfo();
		IndexInfo projectUid = index(identityIndexes, "uk_firebase_identities_project_uid");
		IndexInfo userId = index(identityIndexes, "uk_firebase_identities_user_id");
		assertThat(projectUid.isUnique()).isTrue();
		assertThat(projectUid.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("firebaseProjectId", "firebaseUid");
		assertThat(userId.isUnique()).isTrue();
		assertThat(userId.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("userId");

		List<IndexInfo> enrollmentIndexes = mongoTemplate
				.indexOps(FirebaseEnrollmentAttempt.class)
				.getIndexInfo();
		IndexInfo pending = index(
				enrollmentIndexes,
				"uk_firebase_enrollment_pending_binding"
		);
		IndexInfo cleanup = index(
				enrollmentIndexes,
				"ttl_firebase_enrollment_cleanup_at"
		);
		assertThat(pending.isUnique()).isTrue();
		assertThat(pending.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly(
						"firebaseProjectId",
						"firebaseUid",
						"bindingType",
						"boundUserId"
				);
		assertThat(cleanup.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("cleanupAt");
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

	private FirebaseIdentity identity(String projectId, String uid, String userId) {
		return FirebaseIdentity.create(projectId, uid, userId, CREATED_AT);
	}

	private FirebaseEnrollmentAttempt directAttempt(Instant createdAt) {
		return FirebaseEnrollmentAttempt.create(
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null,
				FirebaseAuthenticationMethod.GOOGLE,
				createdAt,
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}

	private FirebaseEnrollmentAttempt guestAttempt(String userId) {
		return FirebaseEnrollmentAttempt.create(
				"test-project",
				"Opaque_UID",
				FirebaseEnrollmentBindingType.GUEST_USER,
				userId,
				FirebaseAuthenticationMethod.GOOGLE,
				CREATED_AT,
				Duration.ofMinutes(10),
				Duration.ofHours(24)
		);
	}
}
