package web.tosunsaeng.identity.domain.auth.session.repository;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import com.mongodb.client.*;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.session.domain.RefreshReissueResponse;
import web.tosunsaeng.identity.domain.user.domain.enums.UserAccountType;

/** Local in-memory Mongo mapping/index/CAS only; not a replica-set transaction test. */
class ReissueRecoveryRepositoryTests {
	MongoServer server; MongoClient client; MongoTemplate mongo;
	RefreshSessionRepository sessions; RefreshReissueResponseRepository responses;
	static final Instant NOW = Instant.parse("2026-09-09T00:00:00Z");
	static final String USER = "00000000-0000-4000-8000-000000000001";
	@BeforeEach void setup() {
		server = new MongoServer(new MemoryBackend()); var address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		mongo = new MongoTemplate(client, "test-reissue-repository");
		for (var type : List.of(RefreshSession.class, RefreshReissueResponse.class)) {
			new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(type)
					.forEach(index -> mongo.indexOps(type).ensureIndex(index));
		}
		var factory = new MongoRepositoryFactory(mongo);
		sessions = factory.getRepository(RefreshSessionRepository.class); responses = factory.getRepository(RefreshReissueResponseRepository.class);
	}
	@AfterEach void close() { client.close(); server.shutdownNow(); }
	RefreshSession active(String user) { return RefreshSession.create(user, "test-hash-" + UUID.randomUUID(), NOW, NOW.plusSeconds(600)); }
	RefreshSession recovered(String user) {
		var session = active(user); session.rotate(NOW, UUID.randomUUID().toString());
		session.recordRecovery("same-test-request-hash", UUID.randomUUID().toString(), NOW, NOW.plusSeconds(120), UserAccountType.GUEST); return session;
	}
	@Test void uniqueRecoveryEvidenceRejectsSameUserRequestIdAndSeparatesUsers() {
		// In-memory backend ignores the partial predicate; use eligible rows only here.
		// Multiple legacy rows under the partial unique index must be checked on a real replica set.
		sessions.save(recovered(USER));
		assertThatThrownBy(() -> sessions.save(recovered(USER))).isInstanceOf(DuplicateKeyException.class);
		sessions.save(recovered(UUID.randomUUID().toString()));
		assertThat(sessions.existsByUserIdAndRotationRequestKeyHash(USER, "same-test-request-hash")).isTrue();
		assertThat(sessions.count()).isEqualTo(2);
	}
	@Test void partialIndexDefinitionExcludesLegacyDocuments() {
		var indexes = new MongoPersistentEntityIndexResolver(mongo.getConverter().getMappingContext()).resolveIndexFor(RefreshSession.class);
		var recoveryIndex = java.util.stream.StreamSupport.stream(indexes.spliterator(), false)
				.filter(i -> "uk_refresh_user_rotation_request".equals(i.getIndexOptions().getString("name"))).findFirst().orElseThrow();
		Document predicate = (Document) recoveryIndex.getIndexOptions().get("partialFilterExpression");
		assertThat(predicate).isEqualTo(Document.parse("{ 'rotationRequestKeyHash': { '$type': 'string' } }"));
		sessions.save(active(USER));
		assertThat(mongo.getCollection("refresh_sessions").countDocuments(predicate)).isZero();
		sessions.save(recovered(USER));
		assertThat(mongo.getCollection("refresh_sessions").countDocuments(predicate)).isEqualTo(1);
	}
	@Test void responseDeletionDoesNotDeleteRotationEvidenceOrShortenSessionExpiry() {
		var source = sessions.save(recovered(USER));
		var doc = new RefreshReissueResponse(source.getRotationResponseId(), source.getSessionId(), "test-key", "test-nonce", "test-ciphertext",
				NOW.plusSeconds(300), NOW.plusSeconds(600), NOW.plusSeconds(120));
		responses.insert(doc); responses.deleteById(doc.getResponseId());
		var stored = sessions.findById(source.getSessionId()).orElseThrow();
		assertThat(stored.getRotationRequestKeyHash()).isEqualTo("same-test-request-hash");
		assertThat(stored.getExpiresAt()).isEqualTo(NOW.plusSeconds(600));
	}
	@Test void sourcePayloadIsUniqueAndTtlIndexIsSeparate() {
		var doc = new RefreshReissueResponse(UUID.randomUUID().toString(), "test-source", "test-key", "test-nonce", "test-ciphertext",
				NOW.plusSeconds(300), NOW.plusSeconds(600), NOW.plusSeconds(120));
		responses.insert(doc);
		assertThatThrownBy(() -> responses.insert(new RefreshReissueResponse(UUID.randomUUID().toString(), "test-source", "test-key", "test-nonce", "test-ciphertext",
				NOW.plusSeconds(300), NOW.plusSeconds(600), NOW.plusSeconds(120)))).isInstanceOf(DuplicateKeyException.class);
		var indexes = mongo.getCollection("refresh_reissue_responses").listIndexes().into(new ArrayList<>());
		assertThat(indexes).anySatisfy(index -> {
			assertThat(index.getString("name")).isEqualTo("ttl_reissue_response");
			assertThat(((Number) index.get("expireAfterSeconds")).longValue()).isZero();
		});
		Document stored = mongo.getCollection("refresh_reissue_responses").find().first();
		assertThat(stored.keySet()).doesNotContain("refreshToken", "accessToken", "response", "key");
	}
	@Test void replayTouchCasRejectsLogoutWinner() {
		var child = sessions.save(active(USER)); var replay = sessions.findById(child.getSessionId()).orElseThrow();
		var logout = sessions.findById(child.getSessionId()).orElseThrow(); logout.logout(NOW.plusSeconds(1)); sessions.save(logout);
		replay.touchForRecovery(NOW.plusSeconds(1)); assertThatThrownBy(() -> sessions.save(replay)).isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(sessions.findById(child.getSessionId()).orElseThrow().isRevoked()).isTrue();
	}
	@Test void replayTouchCasRejectsRotationWinner() {
		var child = sessions.save(active(USER)); var replay = sessions.findById(child.getSessionId()).orElseThrow();
		var rotation = sessions.findById(child.getSessionId()).orElseThrow(); rotation.rotate(NOW.plusSeconds(1), UUID.randomUUID().toString()); sessions.save(rotation);
		replay.touchForRecovery(NOW.plusSeconds(1)); assertThatThrownBy(() -> sessions.save(replay)).isInstanceOf(OptimisticLockingFailureException.class);
	}
}
