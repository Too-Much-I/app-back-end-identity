package web.tosunsaeng.identity.domain.auth.phoneidentity.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Instant;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneEligibilityBindingRevision;

class PhoneEligibilityBindingRevisionRepositoryIntegrationTests {

	private MongoServer server;
	private MongoClient client;
	private PhoneEligibilityBindingRevisionRepository repository;

	@BeforeEach
	void setUp() {
		server = new MongoServer(new MemoryBackend());
		InetSocketAddress address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		MongoTemplate template = new MongoTemplate(client, "binding-revision-test");
		new MongoPersistentEntityIndexResolver(template.getConverter().getMappingContext())
				.resolveIndexFor(PhoneEligibilityBindingRevision.class)
				.forEach(index -> template.indexOps(PhoneEligibilityBindingRevision.class).ensureIndex(index));
		MongoRepositoryFactory factory = new MongoRepositoryFactory(template);
		repository = factory.getRepository(
				PhoneEligibilityBindingRevisionRepository.class,
				RepositoryFragments.just(new PhoneEligibilityBindingRevisionRepositoryCustomImpl(template)));
	}

	@AfterEach
	void tearDown() {
		if (client != null) client.close();
		if (server != null) server.shutdownNow();
	}

	@Test
	void revisionAdvancesMonotonicallyAndRevocationUsesExpectedRevision() {
		String userId = "00000000-0000-4000-8000-000000000096";
		Instant now = Instant.parse("2026-08-14T00:00:00Z");
		PhoneEligibilityBindingRevision first = repository.advanceVerified(
				userId, "opaque-scope-v1", now);
		PhoneEligibilityBindingRevision second = repository.advanceVerified(
				userId, "opaque-scope-v1", now.plusSeconds(1));

		assertThat(first.getRevision()).isEqualTo(1);
		assertThat(second.getRevision()).isEqualTo(2);
		assertThat(repository.advanceRevoked(
				userId, "opaque-scope-v1", 1, now.plusSeconds(2))).isEmpty();
		assertThat(repository.advanceRevoked(
				userId, "opaque-scope-v1", 2, now.plusSeconds(2)))
				.hasValueSatisfying(revoked -> {
					assertThat(revoked.getRevision()).isEqualTo(3);
					assertThat(revoked.isActive()).isFalse();
				});
	}
}
