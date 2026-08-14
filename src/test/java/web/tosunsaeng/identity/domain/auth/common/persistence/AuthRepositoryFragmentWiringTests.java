package web.tosunsaeng.identity.domain.auth.common.persistence;

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
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import web.tosunsaeng.identity.domain.auth.domain.enums.FirebaseEnrollmentBindingType;
import web.tosunsaeng.identity.domain.auth.federation.repository.FirebaseEnrollmentAttemptRepository;
import web.tosunsaeng.identity.domain.auth.phoneidentity.domain.PhoneFingerprint;
import web.tosunsaeng.identity.domain.auth.phoneidentity.repository.PhoneFingerprintAliasRepository;
import web.tosunsaeng.identity.domain.auth.session.repository.RefreshSessionRepository;

class AuthRepositoryFragmentWiringTests {

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		MongoTemplate mongoTemplate = new MongoTemplate(
				mongoClient,
				"auth-repository-wiring-test"
		);

		context = new AnnotationConfigApplicationContext();
		context.registerBean("mongoTemplate", MongoTemplate.class, () -> mongoTemplate);
		context.register(RepositoryConfiguration.class);
		context.refresh();
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
		if (mongoClient != null) {
			mongoClient.close();
		}
		if (mongoServer != null) {
			mongoServer.shutdownNow();
		}
	}

	@Test
	void discoversMovedRepositoriesAndTheirCustomFragments() {
		FirebaseEnrollmentAttemptRepository enrollmentRepository = context.getBean(
				FirebaseEnrollmentAttemptRepository.class
		);
		PhoneFingerprintAliasRepository aliasRepository = context.getBean(
				PhoneFingerprintAliasRepository.class
		);

		assertThat(enrollmentRepository.findPendingByBinding(
				"test-project",
				"opaque-test-uid",
				FirebaseEnrollmentBindingType.DIRECT_SIGNUP,
				null
		)).isEmpty();
		assertThat(enrollmentRepository.expireIfPendingAndExpired(
				"a10ef8a4-24d5-42e6-8966-a519d27d0321",
				Instant.parse("2026-08-14T09:00:00Z")
		)).isFalse();
		assertThat(aliasRepository.findAllActiveByFingerprints(List.of(
				new PhoneFingerprint("v1", "A".repeat(43))
		))).isEmpty();
		assertThat(context.getBean(RefreshSessionRepository.class)).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableMongoRepositories(basePackageClasses = {
			FirebaseEnrollmentAttemptRepository.class,
			PhoneFingerprintAliasRepository.class,
			RefreshSessionRepository.class
	})
	static class RepositoryConfiguration {
	}
}
