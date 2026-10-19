package web.tosunsaeng.identity.domain.auth.ownerevent.repository;

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
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.ownerevent.application.OwnerEventCaptureService;

class OwnerEventRepositoryIntegrationTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
	private static final String SOURCE = "00000000-0000-4000-8000-000000000001";
	private static final String TARGET = "00000000-0000-4000-8000-000000000002";
	private MongoServer server;
	private MongoClient client;
	private MongoTemplate template;
	private OwnerEventCoreRepository cores;
	private OwnerEventDeliveryRepository deliveries;
	private OwnerEventConsumerStateRepository states;

	@BeforeEach
	void setUp() {
		server = new MongoServer(new MemoryBackend());
		InetSocketAddress address = server.bind();
		client = MongoClients.create("mongodb://" + address.getHostString() + ":" + address.getPort());
		template = new MongoTemplate(client, "owner-event-test");
		ensureIndexes(OwnerEventCore.class);
		ensureIndexes(OwnerEventDelivery.class);
		ensureIndexes(OwnerEventConsumerState.class);
		ensureIndexes(PhoneRejoinLineage.class);
		MongoRepositoryFactory factory = new MongoRepositoryFactory(template);
		cores = factory.getRepository(OwnerEventCoreRepository.class,
				RepositoryFragments.just(new OwnerEventCoreRepositoryCustomImpl(template)));
		deliveries = factory.getRepository(OwnerEventDeliveryRepository.class,
				RepositoryFragments.just(new OwnerEventDeliveryRepositoryCustomImpl(template)));
		states = factory.getRepository(OwnerEventConsumerStateRepository.class,
				RepositoryFragments.just(new OwnerEventConsumerStateRepositoryCustomImpl(template)));
	}

	@AfterEach
	void tearDown() {
		if (client != null) client.close();
		if (server != null) server.shutdownNow();
	}

	@Test
	void captureAllocatesMonotonicPerConsumerSequencesAndExactDeliveries() {
		OwnerEventCaptureService capture = new OwnerEventCaptureService(cores, deliveries, states);
		OwnerEventCore first = capture.captureUserMerged(SOURCE, TARGET, NOW);
		OwnerEventCore second = capture.captureTrialOwnerRebind(
				"00000000-0000-4000-8000-000000000003",
				"00000000-0000-4000-8000-000000000004", "FREE_EXAM_ONCE", 2, 1,
				"00000000-0000-4000-8000-000000000005", NOW.plusSeconds(1));

		assertThat(deliveries.findAllByEventId(first.getEventId()))
				.extracting(OwnerEventDelivery::getConsumer)
				.containsExactlyInAnyOrder(OwnerEventConsumer.BILLING, OwnerEventConsumer.LEARNING_CORE);
		assertThat(deliveries.findAllByEventId(second.getEventId()))
				.singleElement().extracting(OwnerEventDelivery::getConsumer)
				.isEqualTo(OwnerEventConsumer.BILLING);
		assertThat(states.findById(OwnerEventConsumer.BILLING)).get()
				.extracting(OwnerEventConsumerState::getLastAllocatedSequence).isEqualTo(2L);
		assertThat(states.findById(OwnerEventConsumer.LEARNING_CORE)).get()
				.extracting(OwnerEventConsumerState::getLastAllocatedSequence).isEqualTo(1L);
	}

	@Test
	void indexesRejectDuplicateUserMergeSource() {
		cores.save(OwnerEventCore.userMerged(SOURCE, TARGET, NOW));
		assertThatThrownBy(() -> cores.save(OwnerEventCore.userMerged(
				SOURCE, "00000000-0000-4000-8000-000000000009", NOW.plusSeconds(1))))
				.isInstanceOf(DuplicateKeyException.class);
	}

	@Test
	void createsUniqueSequenceAndNullableTtlIndexes() {
		List<IndexInfo> deliveryIndexes = template.indexOps(OwnerEventDelivery.class).getIndexInfo();
		assertThat(index(deliveryIndexes, "uk_owner_delivery_consumer_sequence").isUnique()).isTrue();
		assertThat(index(deliveryIndexes, "ttl_owner_event_delivery_cleanup_at").getExpireAfter())
				.hasValue(java.time.Duration.ZERO);
		assertThat(index(template.indexOps(OwnerEventCore.class).getIndexInfo(),
				"uk_owner_event_user_merged_source").getPartialFilterExpression())
				.contains("USER_MERGED");
	}

	private void ensureIndexes(Class<?> type) {
		new MongoPersistentEntityIndexResolver(template.getConverter().getMappingContext())
				.resolveIndexFor(type).forEach(index -> template.indexOps(type).ensureIndex(index));
	}

	private IndexInfo index(List<IndexInfo> indexes, String name) {
		return indexes.stream().filter(value -> name.equals(value.getName())).findFirst().orElseThrow();
	}
}
