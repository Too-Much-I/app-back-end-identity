package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;

class OwnerEventCaptureServiceTests {
	private static final String SOURCE = "00000000-0000-4000-8000-000000000001";
	private static final String TARGET = "00000000-0000-4000-8000-000000000002";
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");

	@Test
	void allocatesIndependentConsumerSequencesForUserMerge() {
		OwnerEventCoreRepository cores = mock(OwnerEventCoreRepository.class);
		OwnerEventDeliveryRepository deliveries = mock(OwnerEventDeliveryRepository.class);
		OwnerEventConsumerStateRepository states = mock(OwnerEventConsumerStateRepository.class);
		when(states.allocateNext(eq(OwnerEventConsumer.BILLING), eq(NOW))).thenReturn(7L);
		when(states.allocateNext(eq(OwnerEventConsumer.LEARNING_CORE), eq(NOW))).thenReturn(3L);
		OwnerEventCaptureService service = new OwnerEventCaptureService(cores, deliveries, states);

		service.captureUserMerged(SOURCE, TARGET, NOW);

		ArgumentCaptor<List<OwnerEventDelivery>> captor = ArgumentCaptor.forClass(List.class);
		verify(deliveries).saveAll(captor.capture());
		assertThat(captor.getValue()).extracting(OwnerEventDelivery::getConsumerSequence)
				.containsExactly(7L, 3L);
	}

	@Test
	void trialRebindAllocatesBillingOnly() {
		OwnerEventCoreRepository cores = mock(OwnerEventCoreRepository.class);
		OwnerEventDeliveryRepository deliveries = mock(OwnerEventDeliveryRepository.class);
		OwnerEventConsumerStateRepository states = mock(OwnerEventConsumerStateRepository.class);
		when(states.allocateNext(any(), any())).thenReturn(1L);
		OwnerEventCaptureService service = new OwnerEventCaptureService(cores, deliveries, states);

		service.captureTrialOwnerRebind(SOURCE, TARGET, "FREE_EXAM_ONCE", 2, 1,
				"00000000-0000-4000-8000-000000000003", NOW);

		ArgumentCaptor<List<OwnerEventDelivery>> captor = ArgumentCaptor.forClass(List.class);
		verify(deliveries).saveAll(captor.capture());
		assertThat(captor.getValue()).singleElement()
				.extracting(OwnerEventDelivery::getConsumer)
				.isEqualTo(OwnerEventConsumer.BILLING);
	}
}
