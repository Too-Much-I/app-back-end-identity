package web.tosunsaeng.identity.domain.auth.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexField;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import web.tosunsaeng.identity.domain.auth.domain.entity.SocialIdentity;
import web.tosunsaeng.identity.domain.auth.domain.enums.SocialProvider;

class SocialIdentityRepositoryIntegrationTests {

	private static final String USER_A = "73a18ed4-1d56-4c4f-afd6-b39175b82a86";
	private static final String USER_B = "85a8c19e-5ab4-4945-bd1e-3a20ac269d9b";
	private static final Instant CREATED_AT = Instant.parse("2026-08-13T01:02:03Z");

	private MongoServer mongoServer;
	private MongoClient mongoClient;
	private MongoTemplate mongoTemplate;
	private SocialIdentityRepository repository;

	@BeforeEach
	void setUp() {
		mongoServer = new MongoServer(new MemoryBackend());
		InetSocketAddress address = mongoServer.bind();
		mongoClient = MongoClients.create(
				"mongodb://" + address.getHostString() + ":" + address.getPort()
		);
		mongoTemplate = new MongoTemplate(mongoClient, "social-identity-test");
		MongoPersistentEntityIndexResolver indexResolver =
				new MongoPersistentEntityIndexResolver(
						mongoTemplate.getConverter().getMappingContext()
				);
		indexResolver.resolveIndexFor(SocialIdentity.class)
				.forEach(indexDefinition -> mongoTemplate
						.indexOps(SocialIdentity.class)
						.ensureIndex(indexDefinition));
		repository = new MongoRepositoryFactory(mongoTemplate)
				.getRepository(SocialIdentityRepository.class);
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
	void storesMultipleProvidersForOneUserAndFindsCanonicalOwner() {
		SocialIdentity google = identity(USER_A, SocialProvider.GOOGLE, "Google_Subject_ABC");
		SocialIdentity kakao = identity(USER_A, SocialProvider.KAKAO, "kakao-subject");
		SocialIdentity apple = identity(USER_A, SocialProvider.APPLE, "apple-subject");

		repository.saveAll(List.of(google, kakao, apple));

		assertThat(repository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"Google_Subject_ABC"
		)).hasValueSatisfying(identity -> assertThat(identity.getUserId()).isEqualTo(USER_A));
		assertThat(repository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"google_subject_abc"
		)).isEmpty();
		assertThat(repository.findAllByUserId(USER_A))
				.extracting(SocialIdentity::getProvider)
				.containsExactlyInAnyOrder(
						SocialProvider.GOOGLE,
						SocialProvider.KAKAO,
						SocialProvider.APPLE
				);
	}

	@Test
	void allowsSameSubjectAcrossDifferentProviders() {
		repository.save(identity(USER_A, SocialProvider.GOOGLE, "same-subject"));
		repository.save(identity(USER_A, SocialProvider.APPLE, "same-subject"));

		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void rejectsSameProviderAndSubjectForDifferentUsersAtDatabaseBoundary() {
		repository.save(identity(USER_A, SocialProvider.GOOGLE, "duplicate-subject"));

		assertThatThrownBy(() -> repository.save(
				identity(USER_B, SocialProvider.GOOGLE, "duplicate-subject")
		)).isInstanceOf(DuplicateKeyException.class);
		assertThat(repository.count()).isEqualTo(1);
		assertThat(repository.findByProviderAndProviderSubject(
				SocialProvider.GOOGLE,
				"duplicate-subject"
		)).hasValueSatisfying(identity -> assertThat(identity.getUserId()).isEqualTo(USER_A));
	}

	@Test
	void createsCompoundUniqueAndUserLookupIndexesWithExpectedKeyOrder() {
		List<IndexInfo> indexes = mongoTemplate.indexOps(SocialIdentity.class).getIndexInfo();

		IndexInfo providerSubjectIndex = indexes.stream()
				.filter(index -> index.getName().equals(
						"uk_social_identities_provider_subject"
				))
				.findFirst()
				.orElseThrow();
		IndexInfo userIdIndex = indexes.stream()
				.filter(index -> index.getName().equals("ix_social_identities_user_id"))
				.findFirst()
				.orElseThrow();

		assertThat(providerSubjectIndex.isUnique()).isTrue();
		assertThat(providerSubjectIndex.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("provider", "providerSubject");
		assertThat(userIdIndex.isUnique()).isFalse();
		assertThat(userIdIndex.getIndexFields())
				.extracting(IndexField::getKey)
				.containsExactly("userId");
	}

	private SocialIdentity identity(
			String userId,
			SocialProvider provider,
			String providerSubject
	) {
		return SocialIdentity.create(userId, provider, providerSubject, CREATED_AT);
	}
}
