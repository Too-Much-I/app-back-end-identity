package web.tosunsaeng.identity.domain.auth.ownerevent.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventConsumerState;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventCore;
import web.tosunsaeng.identity.domain.auth.domain.entity.OwnerEventDelivery;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventCircuitStatus;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventConsumer;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventFailureCode;
import web.tosunsaeng.identity.domain.auth.domain.enums.OwnerEventType;
import web.tosunsaeng.identity.domain.auth.ownerevent.infrastructure.OwnerEventProperties;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventConsumerStateRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventCoreRepository;
import web.tosunsaeng.identity.domain.auth.ownerevent.repository.OwnerEventDeliveryRepository;
import web.tosunsaeng.identity.global.workload.WorkloadDeliveryResult;

public final class OwnerEventPublisher {
	private static final Logger log = LoggerFactory.getLogger(OwnerEventPublisher.class);
	public enum Outcome {
		NONE, PUBLISHED, RETRY_SCHEDULED, DEAD_LETTERED, CIRCUIT_PAUSED,
		BLOCKED_BY_DISABLED_CHANNEL, LEASE_LOST
	}

	private final OwnerEventConsumer consumer;
	private final String leaseOwner = UUID.randomUUID().toString();
	private final OwnerEventProperties properties;
	private final OwnerEventCoreRepository coreRepository;
	private final OwnerEventDeliveryRepository deliveryRepository;
	private final OwnerEventConsumerStateRepository stateRepository;
	private final OwnerEventPublishTransactionService transactionService;
	private final OwnerEventWireMapper mapper;
	private final OwnerEventDeliveryPort deliveryPort;
	private final OwnerEventRetryPolicy retryPolicy;
	private final MeterRegistry meterRegistry;
	private final Clock clock;

	public OwnerEventPublisher(
			OwnerEventConsumer consumer, OwnerEventProperties properties,
			OwnerEventCoreRepository coreRepository,
			OwnerEventDeliveryRepository deliveryRepository,
			OwnerEventConsumerStateRepository stateRepository,
			OwnerEventPublishTransactionService transactionService,
			OwnerEventWireMapper mapper, OwnerEventDeliveryPort deliveryPort,
			OwnerEventRetryPolicy retryPolicy, MeterRegistry meterRegistry, Clock clock
	) {
		this.consumer = Objects.requireNonNull(consumer);
		this.properties = Objects.requireNonNull(properties);
		this.coreRepository = Objects.requireNonNull(coreRepository);
		this.deliveryRepository = Objects.requireNonNull(deliveryRepository);
		this.stateRepository = Objects.requireNonNull(stateRepository);
		this.transactionService = Objects.requireNonNull(transactionService);
		this.mapper = Objects.requireNonNull(mapper);
		this.deliveryPort = Objects.requireNonNull(deliveryPort);
		this.retryPolicy = Objects.requireNonNull(retryPolicy);
		this.meterRegistry = Objects.requireNonNull(meterRegistry);
		this.clock = Objects.requireNonNull(clock);
	}

	public Outcome publishNext() {
		Optional<OwnerEventConsumerState> stateOptional = stateRepository.findById(consumer);
		if (stateOptional.isEmpty()) return Outcome.NONE;
		OwnerEventConsumerState state = stateOptional.orElseThrow();
		if (state.getCircuitStatus() == OwnerEventCircuitStatus.PAUSED) return Outcome.CIRCUIT_PAUSED;
		long nextSequence = state.getLastPublishedSequence() + 1;
		Optional<OwnerEventDelivery> head = deliveryRepository
				.findByConsumerAndConsumerSequence(consumer, nextSequence);
		if (head.isEmpty()) return Outcome.NONE;
		OwnerEventCore core = coreRepository.findById(head.orElseThrow().getEventId())
				.orElseThrow(() -> new IllegalStateException("Owner event core is missing"));
		if (!channelEnabled(core.getEventType())) {
			return record(core, Outcome.BLOCKED_BY_DISABLED_CHANNEL, null);
		}
		Instant now = clock.instant();
		Optional<OwnerEventDelivery> claimed = deliveryRepository.claimExact(
				consumer, nextSequence, leaseOwner, now, now.plus(properties.getLeaseDuration()));
		if (claimed.isEmpty()) return Outcome.NONE;
		OwnerEventDelivery delivery = claimed.orElseThrow();
		try {
			WorkloadDeliveryResult result = deliveryPort.deliver(core, mapper.serialize(core));
			return handleStatus(core, delivery, result, now);
		} catch (IllegalArgumentException exception) {
			return deadLetter(core, delivery, OwnerEventFailureCode.INVALID_PAYLOAD, now);
		} catch (OwnerEventDeliveryException exception) {
			OwnerEventFailureCode code = switch (exception.kind()) {
				case CREDENTIAL_UNAVAILABLE -> OwnerEventFailureCode.CREDENTIAL_UNAVAILABLE;
				case TIMEOUT -> OwnerEventFailureCode.TIMEOUT;
				case CONNECTION -> OwnerEventFailureCode.CONNECTION_ERROR;
			};
			return retryOrDeadLetter(core, delivery, code, now, null);
		} catch (RuntimeException exception) {
			return retryOrDeadLetter(core, delivery, OwnerEventFailureCode.DELIVERY_ERROR, now, null);
		}
	}

	private Outcome handleStatus(OwnerEventCore core, OwnerEventDelivery delivery,
			WorkloadDeliveryResult result, Instant now) {
		int status = result.statusCode();
		if (status >= 200 && status < 300) {
			boolean completed = transactionService.complete(delivery, leaseOwner, now,
					now.plus(properties.getPublishedRetention()));
			return record(core, completed ? Outcome.PUBLISHED : Outcome.LEASE_LOST, null);
		}
		OwnerEventFailureCode code = statusCode(status);
		if (status == 408 || status == 425 || status == 429 || status >= 500) {
			return retryOrDeadLetter(core, delivery, code, now, result.retryAfterSeconds());
		}
		if (status == 400 || status == 409 || status == 413
				|| status == 415 || status == 422) {
			return deadLetter(core, delivery, code, now);
		}
		boolean paused = transactionService.pause(delivery, leaseOwner, code, now);
		return record(core, paused ? Outcome.CIRCUIT_PAUSED : Outcome.LEASE_LOST, code);
	}

	private Outcome retryOrDeadLetter(OwnerEventCore core, OwnerEventDelivery delivery,
			OwnerEventFailureCode code, Instant now, Integer retryAfterSeconds) {
		if (delivery.getAttemptCount() >= properties.getMaxAttempts()) {
			return deadLetter(core, delivery, code, now);
		}
		Duration local = retryPolicy.delay(delivery.getAttemptCount());
		Duration requested = retryAfterSeconds == null ? Duration.ZERO
				: Duration.ofSeconds(retryAfterSeconds);
		Duration delay = local.compareTo(requested) >= 0 ? local : requested;
		boolean updated = deliveryRepository.scheduleRetry(delivery.getDeliveryId(), leaseOwner,
				code, now.plus(delay));
		return record(core, updated ? Outcome.RETRY_SCHEDULED : Outcome.LEASE_LOST, code);
	}

	private Outcome deadLetter(OwnerEventCore core, OwnerEventDelivery delivery,
			OwnerEventFailureCode code, Instant now) {
		boolean updated = deliveryRepository.markDeadLetter(delivery.getDeliveryId(), leaseOwner,
				code, now, now.plus(properties.getDeadLetterReview()));
		return record(core, updated ? Outcome.DEAD_LETTERED : Outcome.LEASE_LOST, code);
	}

	private boolean channelEnabled(OwnerEventType type) {
		if (consumer == OwnerEventConsumer.LEARNING_CORE) {
			return type == OwnerEventType.USER_MERGED
					&& properties.isLearningCoreUserMergedPublisherEnabled();
		}
		return type == OwnerEventType.USER_MERGED
				? properties.isBillingUserMergedPublisherEnabled()
				: properties.isBillingTrialRebindPublisherEnabled();
	}

	private Outcome record(OwnerEventCore core, Outcome outcome, OwnerEventFailureCode failure) {
		meterRegistry.counter("identity.owner_event.publisher",
				"consumer", consumer.name(), "eventType", core.getEventType().name(),
				"outcome", outcome.name(), "failureCode", failure == null ? "NONE" : failure.name())
				.increment();
		log.atInfo()
				.addKeyValue("event", "owner_event_delivery")
				.addKeyValue("consumer", consumer.name())
				.addKeyValue("eventType", core.getEventType().name())
				.addKeyValue("eventId", core.getEventId())
				.addKeyValue("outcome", outcome.name())
				.addKeyValue("failureCode", failure == null ? "NONE" : failure.name())
				.log("Owner event delivery outcome");
		return outcome;
	}

	private static OwnerEventFailureCode statusCode(int status) {
		return switch (status) {
			case 400 -> OwnerEventFailureCode.HTTP_400;
			case 401 -> OwnerEventFailureCode.HTTP_401;
			case 403 -> OwnerEventFailureCode.HTTP_403;
			case 404 -> OwnerEventFailureCode.HTTP_404;
			case 405 -> OwnerEventFailureCode.HTTP_405;
			case 408 -> OwnerEventFailureCode.HTTP_408;
			case 409 -> OwnerEventFailureCode.HTTP_409;
			case 413 -> OwnerEventFailureCode.HTTP_413;
			case 415 -> OwnerEventFailureCode.HTTP_415;
			case 422 -> OwnerEventFailureCode.HTTP_422;
			case 425 -> OwnerEventFailureCode.HTTP_425;
			case 429 -> OwnerEventFailureCode.HTTP_429;
			default -> status >= 300 && status < 400 ? OwnerEventFailureCode.HTTP_3XX
					: status >= 500 ? OwnerEventFailureCode.HTTP_5XX
					: OwnerEventFailureCode.HTTP_OTHER;
		};
	}
}
