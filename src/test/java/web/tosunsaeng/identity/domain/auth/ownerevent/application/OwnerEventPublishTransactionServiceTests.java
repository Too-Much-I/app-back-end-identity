package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.entity.PhoneRejoinLineage;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventDeliveryStatus;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.PhoneRejoinLineageRepository;

class OwnerEventPublishTransactionServiceTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

	@Test
	void completionAdvancesCursorAndStartsDeliveryLineageAndCoreRetention() {
		OwnerEventCoreRepository cores = mock(OwnerEventCoreRepository.class);
		OwnerEventDeliveryRepository deliveries = mock(OwnerEventDeliveryRepository.class);
		OwnerEventConsumerStateRepository states = mock(OwnerEventConsumerStateRepository.class);
		PhoneRejoinLineageRepository lineages = mock(PhoneRejoinLineageRepository.class);
		PhoneRejoinLineage lineage = PhoneRejoinLineage.available(
				"00000000-0000-4000-8000-000000000001",
				"00000000-0000-4000-8000-000000000002",
				"00000000-0000-4000-8000-000000000003",
				"FREE_EXAM_ONCE", 2, NOW.minusSeconds(60));
		OwnerEventCore core = OwnerEventCore.trialOwnerRebindApproved(
				lineage.getSourceUserId(), "00000000-0000-4000-8000-000000000004",
				"FREE_EXAM_ONCE", 2, 1, lineage.getLineageId(), NOW.minusSeconds(30));
		lineage.consume(core.getEventId(), core.getTargetUserId(), 1, NOW.minusSeconds(30));
		OwnerEventDelivery delivery = OwnerEventDelivery.create(
				core.getEventId(), OwnerEventConsumer.BILLING, 1, NOW.minusSeconds(30));
		Instant deliveryCleanup = NOW.plus(Duration.ofDays(30));
		OwnerEventDelivery storedPublished = OwnerEventDelivery.create(
				core.getEventId(), OwnerEventConsumer.BILLING, 1, NOW.minusSeconds(30));
		ReflectionTestUtils.setField(storedPublished, "status", OwnerEventDeliveryStatus.PUBLISHED);
		ReflectionTestUtils.setField(storedPublished, "cleanupAt", deliveryCleanup);
		when(deliveries.markPublished(delivery.getDeliveryId(), "worker", NOW, deliveryCleanup))
				.thenReturn(true);
		when(states.advancePublished(OwnerEventConsumer.BILLING, 0, NOW)).thenReturn(true);
		when(cores.findById(core.getEventId())).thenReturn(Optional.of(core));
		when(lineages.findByClaimedEventId(core.getEventId())).thenReturn(Optional.of(lineage));
		when(deliveries.countByEventIdAndStatusNot(
				core.getEventId(), OwnerEventDeliveryStatus.PUBLISHED)).thenReturn(0L);
		when(deliveries.findAllByEventId(core.getEventId())).thenReturn(List.of(storedPublished));
		OwnerEventPublishTransactionService service = new OwnerEventPublishTransactionService(
				deliveries, states, cores, lineages);

		assertThat(service.complete(delivery, "worker", NOW, deliveryCleanup)).isTrue();
		assertThat(lineage.getCleanupAt()).isEqualTo(deliveryCleanup);
		verify(lineages).save(lineage);
		verify(cores).markAllPublished(
				core.getEventId(), NOW, deliveryCleanup.plus(Duration.ofHours(24)));
	}
}
