package web.tosunsaeng.identity.domain.auth.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
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
import org.springframework.data.mongodb.core.MongoTemplate;

import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseEnrollmentAttempt;
import web.tosunsaeng.identity.domain.auth.domain.entity.FirebaseIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneFingerprintAlias;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneIdentity;
import web.tosunsaeng.identity.domain.auth.domain.entity.RefreshSession;
import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;

class AuthPersistentTypeCompatibilityTests {

	private static final String LEGACY_ENTITY_PACKAGE =
			"web.tosunsaeng.identity.domain.auth.domain.entity.";
	private static final Instant CREATED_AT = Instant.parse("2026-08-14T09:00:00Z");

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private MongoTemplate mongoTemplate;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		mongoTemplate = new MongoTemplate(mongoClient, "auth-persistent-type-test");
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
	void keepsLegacyPersistentEntityFqcnsAfterCapabilityRefactor() {
		assertThat(List.of(
				FirebaseEnrollmentAttempt.class.getName(),
				FirebaseIdentity.class.getName(),
				PhoneFingerprintAlias.class.getName(),
				PhoneIdentity.class.getName(),
				RefreshSession.class.getName(),
				SocialIdentity.class.getName()
		)).containsExactly(
				LEGACY_ENTITY_PACKAGE + "FirebaseEnrollmentAttempt",
				LEGACY_ENTITY_PACKAGE + "FirebaseIdentity",
				LEGACY_ENTITY_PACKAGE + "PhoneFingerprintAlias",
				LEGACY_ENTITY_PACKAGE + "PhoneIdentity",
				LEGACY_ENTITY_PACKAGE + "RefreshSession",
				LEGACY_ENTITY_PACKAGE + "SocialIdentity"
		);
	}

	@Test
	void writesAndReadsLegacyMongoClassDiscriminator() {
		FirebaseIdentity identity = FirebaseIdentity.create(
				"test-project",
				"opaque-test-uid",
				"73a18ed4-1d56-4c4f-afd6-b39175b82a86",
				CREATED_AT
		);
		Document stored = new Document();
		mongoTemplate.getConverter().write(identity, stored);

		assertThat(stored.getString("_class"))
				.isEqualTo(LEGACY_ENTITY_PACKAGE + "FirebaseIdentity");

		Object restored = mongoTemplate.getConverter().read(Object.class, stored);
		assertThat(restored).isInstanceOf(FirebaseIdentity.class);
		assertThat(((FirebaseIdentity) restored).getUserId()).isEqualTo(identity.getUserId());
	}
}
