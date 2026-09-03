package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventCircuitStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

class OwnerEventPublisherTests {
	private static final Instant NOW = Instant.parse("2026-09-03T00:00:00Z");
	private static final String SOURCE = "00000000-0000-4000-8000-000000000001";
	private static final String TARGET = "00000000-0000-4000-8000-000000000002";

	@Test
	void publishesOnlyExactNextSequence() {
		Fixture fixture = fixture((event, payload) -> new WorkloadDeliveryResult(204, null));
		when(fixture.transaction.complete(eq(fixture.delivery), any(), eq(NOW),
				eq(NOW.plus(Duration.ofDays(30))))).thenReturn(true);

		assertThat(fixture.publisher.publishNext()).isEqualTo(OwnerEventPublisher.Outcome.PUBLISHED);
		verify(fixture.deliveries).findByConsumerAndConsumerSequence(OwnerEventConsumer.BILLING, 10);
		verify(fixture.deliveries).claimExact(eq(OwnerEventConsumer.BILLING), eq(10L),
				any(), eq(NOW), eq(NOW.plusSeconds(60)));
	}

	@Test
	void serverRetryAfterGreaterThanLocalBackoffControlsRetry() {
		Fixture fixture = fixture((event, payload) -> new WorkloadDeliveryResult(503, 300));
		when(fixture.deliveries.scheduleRetry(eq(fixture.delivery.getDeliveryId()), any(),
				eq(OwnerEventFailureCode.HTTP_5XX), eq(NOW.plusSeconds(300))))
				.thenReturn(true);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.RETRY_SCHEDULED);
	}

	@Test
	void authenticationOrRouteFailurePausesCircuitWithoutTerminalDelivery() {
		Fixture fixture = fixture((event, payload) -> new WorkloadDeliveryResult(403, null));
		when(fixture.transaction.pause(eq(fixture.delivery), any(),
				eq(OwnerEventFailureCode.HTTP_403), eq(NOW))).thenReturn(true);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.CIRCUIT_PAUSED);
		verify(fixture.deliveries, never()).markDeadLetter(any(), any(), any(), any(), any());
	}

	@Test
	void unsupportedMediaTypeIsDeadLetteredAsContractFailure() {
		Fixture fixture = fixture((event, payload) -> new WorkloadDeliveryResult(415, null));
		when(fixture.deliveries.markDeadLetter(
				eq(fixture.delivery.getDeliveryId()), any(),
				eq(OwnerEventFailureCode.HTTP_415), eq(NOW),
				eq(NOW.plus(Duration.ofDays(90))))).thenReturn(true);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.DEAD_LETTERED);
	}

	@Test
	void billingSuccessDoesNotCompleteFailedLearningCoreDelivery() {
		OwnerEventProperties properties = new OwnerEventProperties();
		properties.setBillingUserMergedPublisherEnabled(true);
		properties.setLearningCoreUserMergedPublisherEnabled(true);
		OwnerEventCoreRepository cores = mock(OwnerEventCoreRepository.class);
		OwnerEventDeliveryRepository deliveries = mock(OwnerEventDeliveryRepository.class);
		OwnerEventConsumerStateRepository states = mock(OwnerEventConsumerStateRepository.class);
		OwnerEventPublishTransactionService transaction = mock(
				OwnerEventPublishTransactionService.class);
		OwnerEventCore core = OwnerEventCore.userMerged(SOURCE, TARGET, NOW);
		OwnerEventDelivery billing = OwnerEventDelivery.create(
				core.getEventId(), OwnerEventConsumer.BILLING, 1, NOW);
		OwnerEventDelivery learningCore = OwnerEventDelivery.create(
				core.getEventId(), OwnerEventConsumer.LEARNING_CORE, 1, NOW);
		ReflectionTestUtils.setField(billing, "attemptCount", 1);
		ReflectionTestUtils.setField(learningCore, "attemptCount", 1);
		OwnerEventConsumerState billingState = activeState();
		OwnerEventConsumerState learningCoreState = activeState();
		when(states.findById(OwnerEventConsumer.BILLING))
				.thenReturn(Optional.of(billingState));
		when(states.findById(OwnerEventConsumer.LEARNING_CORE))
				.thenReturn(Optional.of(learningCoreState));
		when(deliveries.findByConsumerAndConsumerSequence(OwnerEventConsumer.BILLING, 1))
				.thenReturn(Optional.of(billing));
		when(deliveries.findByConsumerAndConsumerSequence(
				OwnerEventConsumer.LEARNING_CORE, 1)).thenReturn(Optional.of(learningCore));
		when(cores.findById(core.getEventId())).thenReturn(Optional.of(core));
		when(deliveries.claimExact(eq(OwnerEventConsumer.BILLING), eq(1L), any(),
				eq(NOW), eq(NOW.plusSeconds(60)))).thenReturn(Optional.of(billing));
		when(deliveries.claimExact(eq(OwnerEventConsumer.LEARNING_CORE), eq(1L), any(),
				eq(NOW), eq(NOW.plusSeconds(60)))).thenReturn(Optional.of(learningCore));
		when(transaction.complete(eq(billing), any(), eq(NOW),
				eq(NOW.plus(Duration.ofDays(30))))).thenReturn(true);
		when(deliveries.scheduleRetry(eq(learningCore.getDeliveryId()), any(),
				eq(OwnerEventFailureCode.HTTP_5XX), eq(NOW.plusSeconds(5))))
				.thenReturn(true);

		OwnerEventPublisher billingPublisher = publisher(
				OwnerEventConsumer.BILLING, properties, cores, deliveries, states,
				transaction, (event, payload) -> new WorkloadDeliveryResult(204, null));
		OwnerEventPublisher learningCorePublisher = publisher(
				OwnerEventConsumer.LEARNING_CORE, properties, cores, deliveries, states,
				transaction, (event, payload) -> new WorkloadDeliveryResult(503, null));

		assertThat(billingPublisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.PUBLISHED);
		assertThat(learningCorePublisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.RETRY_SCHEDULED);
		verify(transaction).complete(eq(billing), any(), eq(NOW), any());
		verify(deliveries).scheduleRetry(eq(learningCore.getDeliveryId()), any(),
				eq(OwnerEventFailureCode.HTTP_5XX), eq(NOW.plusSeconds(5)));
	}

	@Test
	void disabledHeadChannelBlocksLaterEventsWithoutClaiming() {
		Fixture fixture = fixture((event, payload) -> new WorkloadDeliveryResult(204, null));
		fixture.properties.setBillingUserMergedPublisherEnabled(false);

		assertThat(fixture.publisher.publishNext())
				.isEqualTo(OwnerEventPublisher.Outcome.BLOCKED_BY_DISABLED_CHANNEL);
		verify(fixture.deliveries, never()).claimExact(any(), anyLong(), any(), any(), any());
	}

	private Fixture fixture(OwnerEventDeliveryPort port) {
		OwnerEventProperties properties = new OwnerEventProperties();
		properties.setBillingUserMergedPublisherEnabled(true);
		OwnerEventCoreRepository cores = mock(OwnerEventCoreRepository.class);
		OwnerEventDeliveryRepository deliveries = mock(OwnerEventDeliveryRepository.class);
		OwnerEventConsumerStateRepository states = mock(OwnerEventConsumerStateRepository.class);
		OwnerEventPublishTransactionService transaction = mock(OwnerEventPublishTransactionService.class);
		OwnerEventConsumerState state = mock(OwnerEventConsumerState.class);
		when(state.getCircuitStatus()).thenReturn(OwnerEventCircuitStatus.ACTIVE);
		when(state.getLastPublishedSequence()).thenReturn(9L);
		when(states.findById(OwnerEventConsumer.BILLING)).thenReturn(Optional.of(state));
		OwnerEventCore core = OwnerEventCore.userMerged(SOURCE, TARGET, NOW);
		OwnerEventDelivery delivery = OwnerEventDelivery.create(
				core.getEventId(), OwnerEventConsumer.BILLING, 10, NOW);
		ReflectionTestUtils.setField(delivery, "attemptCount", 1);
		when(deliveries.findByConsumerAndConsumerSequence(OwnerEventConsumer.BILLING, 10))
				.thenReturn(Optional.of(delivery));
		when(cores.findById(core.getEventId())).thenReturn(Optional.of(core));
		when(deliveries.claimExact(eq(OwnerEventConsumer.BILLING), eq(10L), any(),
				eq(NOW), eq(NOW.plusSeconds(60)))).thenReturn(Optional.of(delivery));
		OwnerEventPublisher publisher = new OwnerEventPublisher(
				OwnerEventConsumer.BILLING, properties, cores, deliveries, states,
				transaction, new OwnerEventWireMapper(new ObjectMapper().findAndRegisterModules()),
				port, new OwnerEventRetryPolicy(Duration.ofSeconds(5), Duration.ofMinutes(15),
						() -> 0.5), new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC));
		return new Fixture(publisher, properties, deliveries, transaction, delivery);
	}

	private OwnerEventPublisher publisher(
			OwnerEventConsumer consumer, OwnerEventProperties properties,
			OwnerEventCoreRepository cores, OwnerEventDeliveryRepository deliveries,
			OwnerEventConsumerStateRepository states,
			OwnerEventPublishTransactionService transaction, OwnerEventDeliveryPort port
	) {
		return new OwnerEventPublisher(
				consumer, properties, cores, deliveries, states, transaction,
				new OwnerEventWireMapper(new ObjectMapper().findAndRegisterModules()), port,
				new OwnerEventRetryPolicy(Duration.ofSeconds(5), Duration.ofMinutes(15),
						() -> 0.5), new SimpleMeterRegistry(), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private OwnerEventConsumerState activeState() {
		OwnerEventConsumerState state = mock(OwnerEventConsumerState.class);
		when(state.getCircuitStatus()).thenReturn(OwnerEventCircuitStatus.ACTIVE);
		when(state.getLastPublishedSequence()).thenReturn(0L);
		return state;
	}

	private record Fixture(
			OwnerEventPublisher publisher,
			OwnerEventProperties properties,
			OwnerEventDeliveryRepository deliveries,
			OwnerEventPublishTransactionService transaction,
			OwnerEventDelivery delivery
	) {}
}
